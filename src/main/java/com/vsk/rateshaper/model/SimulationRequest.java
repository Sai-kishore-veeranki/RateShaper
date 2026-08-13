package com.vsk.rateshaper.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for starting a new simulation.
 *
 * <p>Algorithm-specific fields are validated programmatically in
 * {@link com.rateshaper.service.SimulationService} so that only the parameters
 * relevant to the chosen algorithm are required.
 */
public class SimulationRequest {

    @NotNull
    private String algorithm;

    @Positive
    private Integer capacity;

    @Positive
    private Double refillRatePerSec;

    @Positive
    private Integer windowSizeMs;

    @Positive
    private Integer requestLimit;

    @NotNull
    private String pattern;

    @NotNull
    @Positive
    private Integer totalRequests;

    @NotNull
    @Positive
    private Integer durationMs;

    // Getters and setters

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Double getRefillRatePerSec() {
        return refillRatePerSec;
    }

    public void setRefillRatePerSec(Double refillRatePerSec) {
        this.refillRatePerSec = refillRatePerSec;
    }

    public Integer getWindowSizeMs() {
        return windowSizeMs;
    }

    public void setWindowSizeMs(Integer windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
    }

    public Integer getRequestLimit() {
        return requestLimit;
    }

    public void setRequestLimit(Integer requestLimit) {
        this.requestLimit = requestLimit;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }
}
