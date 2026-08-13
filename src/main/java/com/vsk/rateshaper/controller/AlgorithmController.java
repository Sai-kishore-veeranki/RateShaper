package com.vsk.rateshaper.controller;


import com.vsk.rateshaper.model.AlgorithmMeta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    private static final List<AlgorithmMeta> ALGORITHMS = List.of(
        new AlgorithmMeta(
            "TOKEN_BUCKET",
            "Token Bucket",
            "Smoothly refills tokens over time. Absorbs bursts up to bucket capacity, then throttles to the refill rate.",
            List.of("capacity", "refillRatePerSec")
        ),
        new AlgorithmMeta(
            "FIXED_WINDOW",
            "Fixed Window Counter",
            "Divides time into fixed windows with a hard per-window limit. Fast but suffers from boundary bursts.",
            List.of("windowSizeMs", "requestLimit")
        ),
        new AlgorithmMeta(
            "SLIDING_LOG",
            "Sliding Window Log",
            "Tracks every request timestamp in a rolling window. Most accurate but highest memory usage.",
            List.of("windowSizeMs", "requestLimit")
        ),
        new AlgorithmMeta(
            "SLIDING_COUNTER",
            "Sliding Window Counter",
            "Approximates the sliding log with two counters. The efficient middle ground between speed and fairness.",
            List.of("windowSizeMs", "requestLimit")
        )
    );

    @GetMapping
    public List<AlgorithmMeta> getAlgorithms() {
        return ALGORITHMS;
    }
}
