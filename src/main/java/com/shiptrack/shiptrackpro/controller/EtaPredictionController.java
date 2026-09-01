package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.EtaPredictionResponse;
import com.shiptrack.shiptrackpro.service.EtaPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eta")
@RequiredArgsConstructor
public class EtaPredictionController {

    private final EtaPredictionService etaPredictionService;

    @PostMapping("/{shipmentId}/predict")
    public ResponseEntity<EtaPredictionResponse> predict(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(etaPredictionService.predict(shipmentId));
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<EtaPredictionResponse> getPrediction(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(etaPredictionService.getPrediction(shipmentId));
    }
}
