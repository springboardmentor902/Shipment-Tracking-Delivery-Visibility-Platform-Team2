package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.EtaPredictionResponse;
import com.shiptrack.shiptrackpro.dto.EtaOverrideRequest;
import com.shiptrack.shiptrackpro.service.EtaPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eta")
@RequiredArgsConstructor
public class EtaPredictionController {

    private final EtaPredictionService etaPredictionService;

    @PostMapping("/{shipmentId}/predict")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<EtaPredictionResponse> predict(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(etaPredictionService.predict(shipmentId));
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<EtaPredictionResponse> getPrediction(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(etaPredictionService.getPrediction(shipmentId));
    }

    @PatchMapping("/{shipmentId}/override")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<EtaPredictionResponse> overridePrediction(
            @PathVariable Long shipmentId,
            @Valid @RequestBody EtaOverrideRequest request
    ) {
        return ResponseEntity.ok(etaPredictionService.overridePrediction(shipmentId, request));
    }
}
