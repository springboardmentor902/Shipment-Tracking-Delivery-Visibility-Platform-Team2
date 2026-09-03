package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.ShipmentRequest;
import com.shiptrack.shiptrackpro.dto.ShipmentResponse;
import com.shiptrack.shiptrackpro.dto.OperatorAssignmentRequest;
import com.shiptrack.shiptrackpro.service.ShipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(shipmentService.createShipment(request));
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {

        return ResponseEntity.ok(
                shipmentService.getAllShipments()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                shipmentService.getShipmentById(id)
        );
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber) {

        return ResponseEntity.ok(
                shipmentService.getShipmentByTrackingNumber(
                        trackingNumber
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequest request) {

        return ResponseEntity.ok(
                shipmentService.updateShipment(id, request)
        );
    }

    /** Assigns an operator; operators may claim a shipment only for themselves. */
    @PatchMapping("/{id}/operator")
    public ResponseEntity<ShipmentResponse> assignOperator(
            @PathVariable Long id,
            @Valid @RequestBody OperatorAssignmentRequest request) {
        return ResponseEntity.ok(shipmentService.assignOperator(id, request.getOperatorId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(
            @PathVariable Long id) {

        shipmentService.deleteShipment(id);

        return ResponseEntity.noContent().build();
    }
}
