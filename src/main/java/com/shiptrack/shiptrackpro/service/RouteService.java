package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.DriverAssignmentRequest;
import com.shiptrack.shiptrackpro.dto.RouteRequest;
import com.shiptrack.shiptrackpro.dto.RouteResponse;

public interface RouteService {

    RouteResponse createRoute(RouteRequest request);

    RouteResponse assignDriver(
            Long shipmentId,
            DriverAssignmentRequest request
    );

    RouteResponse getRouteForShipment(Long shipmentId);
}
