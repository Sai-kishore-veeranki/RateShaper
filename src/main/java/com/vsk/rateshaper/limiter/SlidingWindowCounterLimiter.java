package com.vsk.rateshaper.limiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Counter rate limiter (approximated).
 *
 * Avoids storing every timestamp by keeping only two counters:
 * {@code currentWindowCount} and {@code previousWindowCount}. The estimated
 * number of requests in the sliding window is computed as:
 *
 * <pre>
 *   overlapFraction = 1 - (timeIntoCurrentWindow / windowSizeMs)
 *   estimatedCount  = currentWindowCount + previousWindowCount * overlapFraction
 * </pre>
 *
 * <p>The request is allowed when {@code estimatedCount < requestLimit}.
 * This is the efficient middle ground — far less memory than the sliding log,
 * yet fairer than the fixed window because it smooths the boundary transition.
 *
 * Thread-safe via per-client state objects stored in a ConcurrentHashMap.
 */
public class SlidingWindowCounterLimiter implements RateLimiter {

    private final long windowSizeMs;
    private final int requestLimit;
    private final ConcurrentHashMap<String, CounterState> counters = new ConcurrentHashMap<>();

    public SlidingWindowCounterLimiter(long windowSizeMs, int requestLimit) {
        this.windowSizeMs = windowSizeMs;
        this.requestLimit = requestLimit;
    }

    private static class CounterState {
        long currentWindowStart;
        int currentWindowCount;
        int previousWindowCount;
    }

    @Override
    public Decision tryAcquire(String clientKey, long nowMillis) {
        CounterState state = counters.computeIfAbsent(clientKey, k -> new CounterState());

        synchronized (state) {
            long currentWindow = nowMillis / windowSizeMs;

            if (state.currentWindowStart == 0) {
                state.currentWindowStart = currentWindow;
            }

            if (currentWindow != state.currentWindowStart) {
                state.previousWindowCount = state.currentWindowCount;
                state.currentWindowCount = 0;
                state.currentWindowStart = currentWindow;
            }

            long timeIntoWindow = nowMillis % windowSizeMs;
            double overlapFraction = 1.0 - (timeIntoWindow / (double) windowSizeMs);
            double estimatedCount = state.currentWindowCount + state.previousWindowCount * overlapFraction;

            if (estimatedCount < requestLimit) {
                state.currentWindowCount++;
                double newEstimated = state.currentWindowCount + state.previousWindowCount * overlapFraction;
                int remaining = (int) Math.max(0, Math.floor(requestLimit - newEstimated));
                return new Decision(true, remaining, "allowed");
            } else {
                int remaining = (int) Math.max(0, Math.floor(requestLimit - estimatedCount));
                return new Decision(false, remaining, "estimated limit exceeded");
            }
        }
    }
}
