package dev.fucksable;

import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一的节流日志输出代理。
 * 按 key 限制日志输出频率，默认 60 秒窗口内同一 key 只输出一次。
 * 也支持永久去重（warnOnce/errorOnce）和转发到外部 Logger（warnTo/errorTo）。
 */
public final class ThrottledLogger {
    private static final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<>();
    private static final long DEFAULT_INTERVAL_MS = 60_000;
    private static final long ONCE = Long.MAX_VALUE;
    private static final int MAX_KEYS = 4096;

    private ThrottledLogger() {}

    /** 默认60秒节流的 warn */
    public static void warn(String key, String message, Object... args) {
        log(key, DEFAULT_INTERVAL_MS, () -> FuckSable.LOGGER.warn(message, args));
    }

    /** 默认60秒节流的 error */
    public static void error(String key, String message, Object... args) {
        log(key, DEFAULT_INTERVAL_MS, () -> FuckSable.LOGGER.error(message, args));
    }

    /** 永久去重的 warn（同一 key 只输出一次） */
    public static void warnOnce(String key, String message, Object... args) {
        log(key, ONCE, () -> FuckSable.LOGGER.warn(message, args));
    }

    /** 转发到外部 Logger 的 warn（用于 @Redirect 场景），默认60秒节流 */
    public static void warnTo(Logger logger, String key, String message, Object... args) {
        log(key, DEFAULT_INTERVAL_MS, () -> logger.warn(message, args));
    }

    /** 转发到外部 Logger 的 error（用于 @Redirect 场景），默认60秒节流 */
    public static void errorTo(Logger logger, String key, String message, Object... args) {
        log(key, DEFAULT_INTERVAL_MS, () -> logger.error(message, args));
    }

    /** 永久去重转发到外部 Logger 的 warn（带 Throwable，用于 @Redirect 场景） */
    public static void warnOnceTo(Logger logger, String key, String message, Throwable t) {
        log(key, ONCE, () -> logger.warn(message, t));
    }

    private static void log(String key, long intervalMs, Runnable action) {
        long now = System.currentTimeMillis();
        Long last = timestamps.get(key);
        if (last != null && (now - last) < intervalMs) return;
        timestamps.put(key, now);
        if (timestamps.size() > MAX_KEYS) {
            timestamps.clear();
            timestamps.put(key, now);
        }
        action.run();
    }
}
