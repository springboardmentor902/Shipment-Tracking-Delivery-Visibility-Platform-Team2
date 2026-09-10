package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.TrackingEventRequest;
import com.shiptrack.shiptrackpro.dto.TrackingEventResponse;
import com.shiptrack.shiptrackpro.service.TrackingEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackingEventController {

    private final TrackingEventService trackingEventService;

    /** SecurityConfig restricts this operational update endpoint to route staff. */
    @PostMapping("/tracking/{shipmentId}")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<TrackingEventResponse> addTrackingEvent(
            @PathVariable Long shipmentId,
            @Valid @RequestBody TrackingEventRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trackingEventService.addTrackingEvent(shipmentId, request));
    }

    /**
     * The shipment-nested alias permits authenticated customers to retrieve
     * their own events while the legacy /api/tracking path remains available
     * for logistics dashboards.
     */
    @GetMapping({
            "/tracking/{shipmentId}",
            "/shipments/{shipmentId}/tracking-events"
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<List<TrackingEventResponse>> getTrackingEvents(
            @PathVariable Long shipmentId
    ) {
        return ResponseEntity.ok(trackingEventService.getTrackingEvents(shipmentId));
    }
}
