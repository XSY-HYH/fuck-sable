package dev.fucksable.mixin;

import dev.fucksable.FuckSable;
import dev.fucksable.fix.CorruptedPointerCache;
import dev.fucksable.fix.FixRegistry;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mixin(SubLevelHoldingChunkMap.class)
public abstract class SubLevelHoldingChunkMapMixin {

    @Unique
    private ExecutorService fucksable$ioExecutor;

    // 收集 saveAll 中的异步磁盘 IO future
    // Collect async disk IO futures from saveAll
    @Unique
    private List<CompletableFuture<Void>> fucksable$pendingIOFutures;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void fucksable$init(CallbackInfo ci) {
        this.fucksable$ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "fuckSable Sub-Level I/O");
            t.setDaemon(true);
            return t;
        });
        this.fucksable$pendingIOFutures = new ArrayList<>();
    }

    // --- async-save 修复项 ---
    // 注意：PalettedContainer 不是线程安全的（有 ThreadingDetector），不能在异步线程调用 ServerLevelPlot.save
    // 因此 saveAll 的序列化部分（含 PalettedContainer.pack）在主线程执行，只把磁盘 IO 放异步线程
    // Note: PalettedContainer is not thread-safe (has ThreadingDetector), ServerLevelPlot.save cannot run on async thread
    // So serialization (including PalettedContainer.pack) runs on main thread, only disk IO goes async
    //
    // v1.7.12 之前的做法是在 saveAll 返回前 join 所有异步磁盘 IO（fucksable$awaitPendingIO），
    // 这会让主线程在每次存档时阻塞等待全部磁盘 IO 完成，表现为存档时服务器卡顿。
    // 这里改为"非阻塞提交 + 屏障 drain"：
    //  - saveAll 只把磁盘 IO 提交到异步线程，不在 saveAll 内阻塞；
    //  - flush 也推迟到异步线程，在所有待写 IO 完成后执行；
    //  - 任何后续磁盘读取（getOrLoadHoldingChunk）以及下一次 saveAll / close 之前，先 drain
    //    （join）尚未完成的异步 IO，保证读取到最新数据，并避免主线程与异步线程并发访问
    //    非线程安全的 regionCache / storageCache。
    // Previously (v1.7.12+) saveAll joined all async disk IO before returning, blocking the main
    // thread on every save and causing a server freeze during saves. Now we use "non-blocking
    // submit + drain barriers":
    //  - saveAll only submits disk IO to the async thread and never blocks inside saveAll;
    //  - flush is also deferred to the async thread, running after all pending writes;
    //  - before any later disk read (getOrLoadHoldingChunk) and before the next saveAll / close,
    //    pending async IO is drained (joined) so reads see the latest data and the non-thread-safe
    //    regionCache / storageCache are never accessed concurrently by both threads.

    @Unique
    private void fucksable$drainPendingIO() {
        if (!FixRegistry.isEnabled("async-save")) return;
        if (this.fucksable$pendingIOFutures == null || this.fucksable$pendingIOFutures.isEmpty()) return;

        CompletableFuture<Void> all = CompletableFuture.allOf(
            this.fucksable$pendingIOFutures.toArray(new CompletableFuture[0])
        );
        this.fucksable$pendingIOFutures.clear();
        try {
            all.join();
        } catch (Exception e) {
            FuckSable.LOGGER.error("Failed to drain async sub-level disk IO", e);
        }
    }

    // 下一次 saveAll 开始前，等待上一次 saveAll 的异步磁盘 IO 完成，避免两次保存的 IO 重叠
    // Before the next saveAll starts, wait for the previous saveAll's async disk IO so two saves never overlap
    @Inject(method = "saveAll", at = @At("HEAD"), remap = false)
    private void fucksable$drainBeforeSaveAll(CallbackInfo ci) {
        this.fucksable$drainPendingIO();
    }

    @Redirect(
        method = "saveAll",
        at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelStorage;attemptSaveSubLevel(Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelData;)V", remap = false),
        remap = false
    )
    private void fucksable$wrapSaveSubLevel(SubLevelStorage storage, GlobalSavedSubLevelPointer pointer, SubLevelData data) {
        if (!FixRegistry.isEnabled("async-save")) {
            try {
                storage.attemptSaveSubLevel(pointer, data);
            } catch (Exception e) {
                FuckSable.LOGGER.error("Failed to save sub-level for pointer {}, skipping", pointer, e);
            }
            return;
        }
        // 把磁盘 IO 提交到异步线程，避免阻塞主线程
        // Submit disk IO to async thread to avoid blocking main thread
        this.fucksable$pendingIOFutures.add(CompletableFuture.runAsync(() -> {
            try {
                storage.attemptSaveSubLevel(pointer, data);
            } catch (Exception e) {
                FuckSable.LOGGER.error("Failed to save sub-level for pointer {}, skipping", pointer, e);
            }
        }, this.fucksable$ioExecutor));
    }

    @Redirect(
        method = "saveAll",
        at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelStorage;attemptSaveHoldingChunk(Lnet/minecraft/world/level/ChunkPos;Ldev/ryanhcode/sable/sublevel/storage/holding/SubLevelHoldingChunk;)V", remap = false),
        remap = false
    )
    private void fucksable$wrapSaveHoldingChunk(SubLevelStorage storage, ChunkPos chunkPos, SubLevelHoldingChunk holdingChunk) {
        if (!FixRegistry.isEnabled("async-save")) {
            try {
                storage.attemptSaveHoldingChunk(chunkPos, holdingChunk);
            } catch (Exception e) {
                FuckSable.LOGGER.error("Failed to save holding chunk at {}, skipping", chunkPos, e);
            }
            return;
        }
        this.fucksable$pendingIOFutures.add(CompletableFuture.runAsync(() -> {
            try {
                storage.attemptSaveHoldingChunk(chunkPos, holdingChunk);
            } catch (Exception e) {
                FuckSable.LOGGER.error("Failed to save holding chunk at {}, skipping", chunkPos, e);
            }
        }, this.fucksable$ioExecutor));
    }

    /**
     * 把 flush 推迟到异步线程，在本次所有待写 IO 完成后执行，避免主线程在存档时等待磁盘刷新。
     * <p>
     * Defer flush to the async thread so it runs after all pending writes of this save,
     * keeping the main thread from waiting on disk sync during saves.
     */
    @Redirect(
        method = "saveAll",
        at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/sublevel/storage/serialization/SubLevelStorage;flush()V", remap = false),
        remap = false
    )
    private void fucksable$deferFlush(SubLevelStorage storage) {
        if (!FixRegistry.isEnabled("async-save")) {
            try {
                storage.flush();
            } catch (Exception e) {
                FuckSable.LOGGER.error("Failed to flush sub-level storage", e);
            }
            return;
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(
            this.fucksable$pendingIOFutures.toArray(new CompletableFuture[0])
        );
        this.fucksable$pendingIOFutures.clear();
        this.fucksable$pendingIOFutures.add(all.thenRunAsync(() -> {
            try {
                storage.flush();
            } catch (Exception e) {
                FuckSable.LOGGER.error("Failed to flush sub-level storage", e);
            }
        }, this.fucksable$ioExecutor));
    }

    // 读取屏障：任何磁盘读取前先 drain，保证读取包含已提交的异步写
    // Read barrier: drain before any disk read so reads include already-submitted async writes
    @Inject(method = "getOrLoadHoldingChunk", at = @At("HEAD"), remap = false)
    private void fucksable$drainBeforeStorageRead(ChunkPos chunkPos, boolean create, CallbackInfoReturnable<SubLevelHoldingChunk> cir) {
        this.fucksable$drainPendingIO();
    }

    // --- corrupted-cleanup 修复项 ---

    /**
     * 在 getOrLoadHoldingChunk 返回前，移除所有已知损坏的指针。
     * 损坏检测和缓存已在 SubLevelStorageMixin 中完成。
     */
    @Inject(method = "getOrLoadHoldingChunk", at = @At("RETURN"), remap = false)
    private void fucksable$cleanupCorruptedPointers(ChunkPos chunkPos, boolean create, CallbackInfoReturnable<SubLevelHoldingChunk> cir) {
        if (!FixRegistry.isEnabled("corrupted-cleanup")) return;

        SubLevelHoldingChunk loadedChunk = cir.getReturnValue();
        if (loadedChunk == null) return;

        Set<SavedSubLevelPointer> knownCorrupted = CorruptedPointerCache.getCorrupted(chunkPos.toLong());
        if (knownCorrupted != null && !knownCorrupted.isEmpty()) {
            List<SavedSubLevelPointer> pointers = loadedChunk.getSubLevelPointers();
            int before = pointers.size();
            pointers.removeAll(knownCorrupted);
            int removed = before - pointers.size();
            if (removed > 0) {
                FuckSable.LOGGER.info("Removed {} corrupted pointer(s) from holding chunk at {}", removed, chunkPos);
            }
        }
    }

    @Inject(method = "close", at = @At("HEAD"), remap = false)
    private void fucksable$awaitBeforeClose(CallbackInfo ci) {
        // 关闭前先 drain 所有待写 IO（含推迟的 flush），确保数据落盘后再关闭 storage 文件
        // Drain all pending IO (including the deferred flush) before storage files are closed
        this.fucksable$drainPendingIO();
        if (this.fucksable$ioExecutor != null) {
            this.fucksable$ioExecutor.shutdown();
            try {
                this.fucksable$ioExecutor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
