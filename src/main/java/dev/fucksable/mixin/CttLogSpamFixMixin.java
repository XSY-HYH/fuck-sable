package dev.fucksable.mixin;

import dev.fucksable.ThrottledLogger;
import dev.fucksable.fix.FixRegistry;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 抑制 CTT (CreateThreadedTrains) 的日志刷屏。
 * <p>
 * 问题：CTT 的 postTick 方法每 tick 都会 poll Future 并 get()，
 * 如果列车计算任务抛出异常（如 NullPointerException、ConcurrentModificationException），
 * 则每 tick 都会输出一条 WARN 日志，导致日志刷屏。
 * <p>
 * 修复方式：拦截 postTick 中的 LOGGER.warn 调用，
 * 通过 ThrottledLogger.warnOnceTo 对同一异常类型只输出一次日志。
 */
@Mixin(targets = "de.mrjulsen.ctt.CreateThreadedTrains", remap = false)
public class CttLogSpamFixMixin {

    /**
     * 拦截 postTick 中的 LOGGER.warn(String, Throwable) 调用，抑制重复日志。
     */
    @Redirect(
        method = "postTick",
        at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V", remap = false),
        remap = false
    )
    private static void fucksable$suppressRepeatedWarn(Logger instance, String message, Throwable arg) {
        if (!FixRegistry.isEnabled("ctt-log-spam-fix")) {
            instance.warn(message, arg);
            return;
        }

        String key = "ctt-warn:" + (arg != null ? arg.getClass().getName() : "unknown");
        ThrottledLogger.warnOnceTo(instance, key, message + " (subsequent errors of this type will be suppressed)", arg);
    }
}
