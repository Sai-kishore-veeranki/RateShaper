package com.vsk.rateshaper.limiter;

/**
 * Result of a rate-limiting decision.
 */
public record Decision(boolean allowed, int remaining, String reason) {
}
