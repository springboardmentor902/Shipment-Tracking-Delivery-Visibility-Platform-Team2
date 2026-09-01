package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.ProofOfDeliveryRequest;
import com.shiptrack.shiptrackpro.dto.ProofOfDeliveryResponse;
import com.shiptrack.shiptrackpro.dto.VerifyProofOfDeliveryRequest;
import com.shiptrack.shiptrackpro.service.ProofOfDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pod")
@RequiredArgsConstructor
public class ProofOfDeliveryController {

    private final ProofOfDeliveryService proofOfDeliveryService;

    @PostMapping(value = "/{shipmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProofOfDeliveryResponse> submit(
            @PathVariable Long shipmentId,
            @Valid @ModelAttribute ProofOfDeliveryRequest request
    ) {
        return ResponseEntity.status(201)
                .body(proofOfDeliveryService.submit(shipmentId, request));
    }

    @PatchMapping("/{shipmentId}/verify")
    public ResponseEntity<ProofOfDeliveryResponse> verify(
            @PathVariable Long shipmentId,
            @Valid @RequestBody VerifyProofOfDeliveryRequest request
    ) {
        return ResponseEntity.ok(proofOfDeliveryService.verify(shipmentId, request));
    }

    @GetMapping("/{shipmentId}")
    public ResponseEntity<ProofOfDeliveryResponse> getForShipment(@PathVariable Long shipmentId) {
        return ResponseEntity.ok(proofOfDeliveryService.getForShipment(shipmentId));
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(proofOfDeliveryService.loadAuthorizedFile(fileName));
    }
}
