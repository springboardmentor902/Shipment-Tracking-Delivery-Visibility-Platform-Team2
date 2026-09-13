package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.DriverAssignmentRequest;
import com.shiptrack.shiptrackpro.dto.RouteRequest;
import com.shiptrack.shiptrackpro.dto.RouteResponse;
import com.shiptrack.shiptrackpro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> createRoute(
            @Valid @RequestBody RouteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routeService.createRoute(request));
    }

    /**
     * Handles both the first driver assignment and later driver changes.
     */
    @PatchMapping("/{shipmentId}/driver")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> assignDriver(
            @PathVariable Long shipmentId,
            @Valid @RequestBody DriverAssignmentRequest request
    ) {
        return ResponseEntity.ok(routeService.assignDriver(shipmentId, request));
    }

    @GetMapping("/{shipmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<RouteResponse> getRouteForShipment(
            @PathVariable Long shipmentId
    ) {
        return ResponseEntity.ok(routeService.getRouteForShipment(shipmentId));
    }

    @GetMapping("/{shipmentId}/history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BUSINESS_CLIENT', 'LOGISTICS_OPERATOR', 'SUPPORT_AGENT', 'ADMINISTRATOR')")
    public ResponseEntity<List<RouteResponse>> getRouteHistoryForShipment(
            @PathVariable Long shipmentId
    ) {
        return ResponseEntity.ok(routeService.getRouteHistoryForShipment(shipmentId));
    }
}
