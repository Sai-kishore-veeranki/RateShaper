package com.vsk.rateshaper.limiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Token Bucket rate limiter.
 *
 * A bucket holds up to {@code capacity} tokens and refills continuously at
 * {@code refillRatePerSec} tokens per second. This produces a smooth traffic
 * shape that can absorb short bursts without dropping requests, as long as
 * tokens are available.
 *
 * Thread-safe via per-client state objects stored in a ConcurrentHashMap.
 */
public class TokenBucketLimiter implements RateLimiter {

    private final int capacity;
    private final double refillRatePerSec;
    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    public TokenBucketLimiter(int capacity, double refillRatePerSec) {
        this.capacity = capacity;
        this.refillRatePerSec = refillRatePerSec;
    }

    private static class BucketState {
        long lastRefillTime;
        double tokens;
    }

    @Override
    public Decision tryAcquire(String clientKey, long nowMillis) {
        BucketState state = buckets.computeIfAbsent(clientKey, k -> new BucketState());

        synchronized (state) {
            // Initialise on first use so the creator thread does not race with nowMillis.
            if (state.lastRefillTime == 0) {
                state.lastRefillTime = nowMillis;
                state.tokens = capacity;
            }

            long elapsed = nowMillis - state.lastRefillTime;
            if (elapsed < 0) {
                elapsed = 0; // protect against non-monotonic clocks
            }
            double elapsedSec = elapsed / 1000.0;
            double tokensToAdd = elapsedSec * refillRatePerSec;

            state.tokens = Math.min(capacity, state.tokens + tokensToAdd);
            state.lastRefillTime = nowMillis;

            if (state.tokens >= 1.0) {
                state.tokens -= 1.0;
                int remaining = (int) Math.floor(state.tokens);
                return new Decision(true, remaining, "allowed");
            } else {
                int remaining = (int) Math.floor(state.tokens);
                return new Decision(false, remaining, "no tokens available");
            }
        }
    }
}
