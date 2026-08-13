package com.vsk.rateshaper.controller;


import com.vsk.rateshaper.model.SimulationRequest;
import com.vsk.rateshaper.service.SimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> startSimulation(@Valid @RequestBody SimulationRequest request) {
        String simulationId = simulationService.createSimulation(request);
        return ResponseEntity.ok(Map.of("simulationId", simulationId));
    }

    @GetMapping("/{simulationId}/stream")
    public SseEmitter stream(@PathVariable String simulationId) {
        return simulationService.startStream(simulationId);
    }
}
