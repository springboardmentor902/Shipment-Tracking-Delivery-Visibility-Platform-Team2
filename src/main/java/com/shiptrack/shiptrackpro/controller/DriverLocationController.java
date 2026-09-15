package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.DriverLocationRequest;
import com.shiptrack.shiptrackpro.dto.LocationUpdateResponse;
import com.shiptrack.shiptrackpro.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class DriverLocationController {

    private final RouteService routeService;

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<LocationUpdateResponse> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody DriverLocationRequest request
    ) {
        return ResponseEntity.ok(routeService.updateLocation(id, request));
    }
}
