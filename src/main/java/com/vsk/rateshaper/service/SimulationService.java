package com.vsk.rateshaper.service;



import com.vsk.rateshaper.limiter.*;
import com.vsk.rateshaper.model.SimulationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SimulationService {

    private final Map<String, SimulationState> simulations = new ConcurrentHashMap<>();

    private static class SimulationState {
        final SimulationRequest request;
        final RateLimiter limiter;

        SimulationState(SimulationRequest request, RateLimiter limiter) {
            this.request = request;
            this.limiter = limiter;
        }
    }

    public String createSimulation(SimulationRequest request) {
        validateRequest(request);
        RateLimiter limiter = createLimiter(request);
        String id = UUID.randomUUID().toString();
        simulations.put(id, new SimulationState(request, limiter));
        return id;
    }

    public SseEmitter startStream(String simulationId) {
        SimulationState state = simulations.get(simulationId);
        if (state == null) {
            throw new IllegalArgumentException("Simulation not found: " + simulationId);
        }

        long timeout = state.request.getDurationMs() + 5000L;
        SseEmitter emitter = new SseEmitter(timeout);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        long[] offsets = computeOffsets(state.request);

        for (int i = 0; i < offsets.length; i++) {
            final int requestId = i + 1;
            final long offset = offsets[i];
            executor.schedule(() -> {
                try {
                    long now = System.currentTimeMillis();
                    Decision decision = state.limiter.tryAcquire("demo-client", now);
                    Map<String, Object> payload = Map.of(
                        "requestId", requestId,
                        "timestampOffsetMs", offset,
                        "allowed", decision.allowed(),
                        "remaining", decision.remaining()
                    );
                    emitter.send(SseEmitter.event().data(payload));
                } catch (Exception e) {
                    // Client disconnected or emitter closed — ignore.
                }
            }, offset, TimeUnit.MILLISECONDS);
        }

        // Complete the emitter and clean up after the simulation duration plus a small buffer.
        executor.schedule(() -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Already completed or timed out.
            }
            simulations.remove(simulationId);
            executor.shutdown();
        }, state.request.getDurationMs() + 100, TimeUnit.MILLISECONDS);

        return emitter;
    }

    private void validateRequest(SimulationRequest req) {
        String algo = req.getAlgorithm();
        if (algo == null) {
            throw new IllegalArgumentException("algorithm is required");
        }

        switch (algo) {
            case "TOKEN_BUCKET" -> {
                if (req.getCapacity() == null) {
                    throw new IllegalArgumentException("Token Bucket requires capacity");
                }
                if (req.getRefillRatePerSec() == null) {
                    throw new IllegalArgumentException("Token Bucket requires refillRatePerSec");
                }
                if (req.getCapacity() <= 0) {
                    throw new IllegalArgumentException("capacity must be positive");
                }
                if (req.getRefillRatePerSec() <= 0) {
                    throw new IllegalArgumentException("refillRatePerSec must be positive");
                }
            }
            case "FIXED_WINDOW", "SLIDING_LOG", "SLIDING_COUNTER" -> {
                if (req.getWindowSizeMs() == null) {
                    throw new IllegalArgumentException("Window-based algorithms require windowSizeMs");
                }
                if (req.getRequestLimit() == null) {
                    throw new IllegalArgumentException("Window-based algorithms require requestLimit");
                }
                if (req.getWindowSizeMs() <= 0) {
                    throw new IllegalArgumentException("windowSizeMs must be positive");
                }
                if (req.getRequestLimit() <= 0) {
                    throw new IllegalArgumentException("requestLimit must be positive");
                }
            }
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algo);
        }

        String pattern = req.getPattern();
        if (pattern == null || (!pattern.equals("BURST") && !pattern.equals("STEADY") && !pattern.equals("RANDOM"))) {
            throw new IllegalArgumentException("pattern must be BURST, STEADY, or RANDOM");
        }

        if (req.getTotalRequests() == null || req.getTotalRequests() <= 0) {
            throw new IllegalArgumentException("totalRequests must be positive");
        }
        if (req.getDurationMs() == null || req.getDurationMs() <= 0) {
            throw new IllegalArgumentException("durationMs must be positive");
        }
    }

    private RateLimiter createLimiter(SimulationRequest req) {
        return (RateLimiter) switch (req.getAlgorithm()) {
            case "TOKEN_BUCKET" ->
                new TokenBucketLimiter(req.getCapacity(), req.getRefillRatePerSec());
            case "FIXED_WINDOW" ->
                new FixedWindowCounterLimiter(req.getWindowSizeMs(), req.getRequestLimit());
            case "SLIDING_LOG" ->
                new SlidingWindowLogLimiter(req.getWindowSizeMs(), req.getRequestLimit());
            case "SLIDING_COUNTER" ->
                new SlidingWindowCounterLimiter(req.getWindowSizeMs(), req.getRequestLimit());
            default -> throw new IllegalArgumentException("Unknown algorithm: " + req.getAlgorithm());
        };
    }

    private long[] computeOffsets(SimulationRequest req) {
        int n = req.getTotalRequests();
        long duration = req.getDurationMs();
        long[] offsets = new long[n];

        switch (req.getPattern()) {
            case "BURST" -> {
                for (int i = 0; i < n; i++) {
                    offsets[i] = (long) (Math.random() * 100);
                }
                Arrays.sort(offsets);
            }
            case "STEADY" -> {
                long spacing = duration / n;
                for (int i = 0; i < n; i++) {
                    offsets[i] = i * spacing;
                }
            }
            case "RANDOM" -> {
                for (int i = 0; i < n; i++) {
                    offsets[i] = (long) (Math.random() * duration);
                }
            }
            default -> throw new IllegalArgumentException("Unknown pattern: " + req.getPattern());
        }
        return offsets;
    }
}
