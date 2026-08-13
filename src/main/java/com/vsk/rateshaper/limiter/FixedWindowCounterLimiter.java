package com.vsk.rateshaper.limiter;



import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed Window Counter rate limiter.
 *
 * Time is divided into fixed windows of {@code windowSizeMs}. Each window has
 * its own counter that allows up to {@code requestLimit} requests. When the
 * window rolls over the counter resets to zero.
 *
 * <p><strong>Classic boundary flaw (deliberately preserved):</strong> a burst
 * that straddles the edge of two adjacent windows can consume up to
 * {@code 2 * requestLimit} requests in a span just slightly longer than
 * {@code windowSizeMs}. This is the well-known weakness that makes the fixed
 * window unfair compared to sliding alternatives.
 *
 * Thread-safe via per-client state objects stored in a ConcurrentHashMap.
 */
public class FixedWindowCounterLimiter implements RateLimiter {

    private final long windowSizeMs;
    private final int requestLimit;
    private final ConcurrentHashMap<String, WindowState> windows = new ConcurrentHashMap<>();

    public FixedWindowCounterLimiter(long windowSizeMs, int requestLimit) {
        this.windowSizeMs = windowSizeMs;
        this.requestLimit = requestLimit;
    }

    private static class WindowState {
        long windowStart;
        int count;
    }

    @Override
    public Decision tryAcquire(String clientKey, long nowMillis) {
        WindowState state = windows.computeIfAbsent(clientKey, k -> new WindowState());

        synchronized (state) {
            long currentWindow = nowMillis / windowSizeMs;

            if (state.windowStart == 0 && state.count == 0) {
                state.windowStart = currentWindow;
            }

            if (currentWindow != state.windowStart) {
                state.windowStart = currentWindow;
                state.count = 0;
            }

            if (state.count < requestLimit) {
                state.count++;
                return new Decision(true, requestLimit - state.count, "allowed");
            } else {
                return new Decision(false, requestLimit - state.count, "window limit exceeded");
            }
        }
    }
}
