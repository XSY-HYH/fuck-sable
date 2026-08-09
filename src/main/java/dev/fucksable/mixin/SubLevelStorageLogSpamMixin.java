package dev.fucksable.mixin;

import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixRegistry;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 限流 Sable 的 SubLevelStorage.attemptLoadSubLevel 在 sub-level 数据缺失时的日志刷屏。
 * <p>
 * 问题分析：
 * 当 sub-level 存储文件中某个 index 的数据缺失或损坏时，attemptLoadSubLevel 会返回 null
 * 并打 ERROR 日志 "Couldn't find sub-level at index {} in storage file for chunk {}"。
 * Sable 会定期重试加载该 sub-level（有延时），导致同一行日志反复刷屏。
 * <p>
 * 修复方式：
 * @Redirect 拦截 LOGGER.error 调用，通过 ThrottledLogger 按 (chunk, index) key 节流：
 * 60 秒内只打一次，既抑制刷屏又保留状态可见性（数据修复后仍能看到日志变化）。
 * <p>
 * 跨版本兼容：attemptLoadSubLevel(ChunkPos, SavedSubLevelPointer) 签名在 Sable 1.x/2.x 一致。
 */
@Mixin(targets = "dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage", remap = false)
public class SubLevelStorageLogSpamMixin {

    @Redirect(
        method = "attemptLoadSubLevel",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"),
        remap = false
    )
    private void fucksable$throttleLog(Logger logger, String format, Object arg1, Object arg2) {
        if (!FixRegistry.isEnabled("sublevel-load-log-spam-fix")) {
            logger.error(format, arg1, arg2);
            return;
        }

        // arg1 = subLevelIndex (Short), arg2 = chunkPos (ChunkPos)
        String key = "sublevel-load:" + arg2 + ":" + arg1;
        ThrottledLogger.errorTo(logger, key, format, arg1, arg2);
    }
}
