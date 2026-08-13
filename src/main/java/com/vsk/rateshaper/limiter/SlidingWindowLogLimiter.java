package com.vsk.rateshaper.limiter;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Log rate limiter.
 *
 * Maintains an ordered log (timestamp deque) of every request per client.
 * On each arrival timestamps older than {@code windowSizeMs} are purged.
 * The request is allowed only if the log size is below {@code requestLimit}.
 *
 * <p><strong>Trade-off:</strong> this is the most accurate of the four
 * algorithms because it tracks every single request, but it is also the most
 * memory-hungry — under heavy load the deque grows proportionally with the
 * request rate. For high-throughput systems the approximated sliding-counter
 * variant is usually preferred.
 *
 * Thread-safe via per-client state objects stored in a ConcurrentHashMap.
 */
public class SlidingWindowLogLimiter implements RateLimiter {

    private final long windowSizeMs;
    private final int requestLimit;
    private final ConcurrentHashMap<String, LogState> logs = new ConcurrentHashMap<>();

    public SlidingWindowLogLimiter(long windowSizeMs, int requestLimit) {
        this.windowSizeMs = windowSizeMs;
        this.requestLimit = requestLimit;
    }

    private static class LogState {
        final ArrayDeque<Long> timestamps = new ArrayDeque<>();
    }

    @Override
    public Decision tryAcquire(String clientKey, long nowMillis) {
        LogState state = logs.computeIfAbsent(clientKey, k -> new LogState());

        synchronized (state) {
            long cutoff = nowMillis - windowSizeMs;
            while (!state.timestamps.isEmpty() && state.timestamps.peekFirst() < cutoff) {
                state.timestamps.pollFirst();
            }

            if (state.timestamps.size() < requestLimit) {
                state.timestamps.addLast(nowMillis);
                return new Decision(true, requestLimit - state.timestamps.size(), "allowed");
            } else {
                return new Decision(false, requestLimit - state.timestamps.size(), "log limit exceeded");
            }
        }
    }
}
