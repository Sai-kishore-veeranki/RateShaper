package com.vsk.rateshaper.limiter;

import com.vsk.rateshaper.limiter.Decision;

/**
 * Core rate limiter interface. All implementations must be thread-safe.
 */
public interface RateLimiter {

    /**
     * Attempt to acquire a permit for the given client.
     *
     * @param clientKey  identifier for the client (e.g. IP, userId)
     * @param nowMillis  current time in milliseconds (monotonic clock not required)
     * @return Decision containing allow/block status, remaining quota, and reason
     */
    Decision tryAcquire(String clientKey, long nowMillis);
}
