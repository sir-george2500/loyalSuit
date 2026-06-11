package com.loyalsuit.common.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A small in-memory fixed-window rate limiter. Each key gets a counter that resets at aligned
 * window boundaries; once the count exceeds the limit within a window, requests are refused
 * until the next one. Counters live per instance — adequate brute-force / abuse protection that
 * degrades gracefully (no external dependency); a multi-node deployment would back this with
 * Redis for a shared view. Memory is bounded by lazily evicting stale keys.
 */
@Component
public class FixedWindowRateLimiter {

    private static final int MAX_KEYS = 50_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** The outcome of a check: whether the call is allowed and, if not, when to retry. */
    public record Decision(boolean allowed, long retryAfterSeconds) {}

    public Decision check(String key, int limit, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        long windowStart = now - Math.floorMod(now, windowSeconds);
        if (windows.size() > MAX_KEYS) {
            windows.values().removeIf(w -> w.start < windowStart);
        }

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.start != windowStart) {
                return new Window(windowStart, 1);
            }
            existing.count++;
            return existing;
        });

        if (window.count <= limit) {
            return new Decision(true, 0);
        }
        return new Decision(false, windowStart + windowSeconds - now);
    }

    private static final class Window {
        private final long start;
        private int count;

        private Window(long start, int count) {
            this.start = start;
            this.count = count;
        }
    }
}
