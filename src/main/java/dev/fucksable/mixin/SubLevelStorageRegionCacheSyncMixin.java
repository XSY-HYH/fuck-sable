package dev.fucksable.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修复 SubLevelStorage.storageCache 的并发访问崩溃（Issue #20）。
 * <p>
 * 问题分析：
 * SubLevelStorage.getRegionStorageFile 使用 Long2ObjectLinkedOpenHashMap（storageCache）
 * 缓存 region storage file。getAndMoveToFirst / removeLast / putAndMoveToFirst 会修改
 * map 的链表结构（prev/next 指针）。当 async-save 修复项启用时，attemptSaveSubLevel
 * 在异步线程执行，调用 getRegionStorageFile 访问 storageCache。如果主线程同时访问
 * storageCache（通过 getOrLoadHoldingChunk -> attemptLoadSubLevel -> getRegionStorageFile），
 * 会导致 Long2ObjectLinkedOpenHashMap 链表状态损坏（某 entry 的 prev/next 指针被设为 -1），
 * 最终抛出 ArrayIndexOutOfBoundsException: Index -1 out of bounds for length N。
 * <p>
 * 修复方式：
 * @Redirect 拦截 getRegionStorageFile 中所有修改 storageCache 链表结构的操作
 * （getAndMoveToFirst / removeLast / putAndMoveToFirst），用 synchronized(map) 保护，
 * 确保 storageCache 的链表结构不会被并发修改。
 * <p>
 * 注意：v1.7.23 的 drain barrier 已经通过在 getOrLoadHoldingChunk 之前 drain 异步 IO
 * 避免了主要的并发场景。这个 mixin 作为底层安全保护，防止任何遗漏的并发访问路径。
 * <p>
 * 崩溃堆栈（Issue #20）：
 * java.lang.ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 33
 *   at Long2ObjectLinkedOpenHashMap.moveIndexToFirst
 *   at Long2ObjectLinkedOpenHashMap.getAndMoveToFirst
 *   at SubLevelStorage.getRegionStorageFile
 *   at SubLevelStorage.attemptSaveSubLevel
 *   at SubLevelHoldingChunkMap.moveAndSaveSubLevel
 *   at SubLevelHoldingChunkMap.saveAll
 */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage", remap = false)
public abstract class SubLevelStorageRegionCacheSyncMixin {

    @Redirect(
        method = "getRegionStorageFile",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;getAndMoveToFirst(J)Ljava/lang/Object;", remap = false),
        remap = false
    )
    private Object fucksable$syncGetAndMoveToFirst(Long2ObjectLinkedOpenHashMap map, long key) {
        synchronized (map) {
            return map.getAndMoveToFirst(key);
        }
    }

    @Redirect(
        method = "getRegionStorageFile",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;removeLast()Ljava/lang/Object;", remap = false),
        remap = false
    )
    private Object fucksable$syncRemoveLast(Long2ObjectLinkedOpenHashMap map) {
        synchronized (map) {
            return map.removeLast();
        }
    }

    @Redirect(
        method = "getRegionStorageFile",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;putAndMoveToFirst(JLjava/lang/Object;)Ljava/lang/Object;", remap = false),
        remap = false
    )
    private Object fucksable$syncPutAndMoveToFirst(Long2ObjectLinkedOpenHashMap map, long key, Object value) {
        synchronized (map) {
            return map.putAndMoveToFirst(key, value);
        }
    }
}
