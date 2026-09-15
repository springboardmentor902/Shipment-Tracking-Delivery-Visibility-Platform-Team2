package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.ShipmentRequest;
import com.shiptrack.shiptrackpro.dto.ShipmentResponse;
import com.shiptrack.shiptrackpro.dto.OperatorAssignmentRequest;
import com.shiptrack.shiptrackpro.dto.TrackingEventRequest;
import com.shiptrack.shiptrackpro.dto.TrackingEventResponse;
import com.shiptrack.shiptrackpro.dto.PublicTrackingResponse;
import com.shiptrack.shiptrackpro.service.ShipmentService;
import com.shiptrack.shiptrackpro.service.TrackingEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final TrackingEventService trackingEventService;

    @GetMapping("/public/tracking/{trackingNumber}")
    public ResponseEntity<PublicTrackingResponse> getPublicTracking(
            @PathVariable String trackingNumber
    ) {
        return ResponseEntity.ok(shipmentService.getPublicTracking(trackingNumber));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT')")
    public ResponseEntity<ShipmentResponse> createShipment(
            @Valid @RequestBody ShipmentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(shipmentService.createShipment(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {

        return ResponseEntity.ok(
                shipmentService.getAllShipments()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> getShipmentById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                shipmentService.getShipmentById(id)
        );
    }

    @GetMapping("/tracking/{trackingNumber}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber) {

        return ResponseEntity.ok(
                shipmentService.getShipmentByTrackingNumber(
                        trackingNumber
                )
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> updateShipment(
            @PathVariable Long id,
            @Valid @RequestBody ShipmentRequest request) {

        return ResponseEntity.ok(
                shipmentService.updateShipment(id, request)
        );
    }

    /** Assigns an operator; operators may claim a shipment only for themselves. */
    @PatchMapping("/{id}/operator")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ShipmentResponse> assignOperator(
            @PathVariable Long id,
            @Valid @RequestBody OperatorAssignmentRequest request) {
        return ResponseEntity.ok(shipmentService.assignOperator(id, request.getOperatorId()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<TrackingEventResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TrackingEventRequest request
    ) {
        return ResponseEntity.ok(trackingEventService.addTrackingEvent(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<Void> deleteShipment(
            @PathVariable Long id) {

        shipmentService.deleteShipment(id);

        return ResponseEntity.noContent().build();
    }
}
