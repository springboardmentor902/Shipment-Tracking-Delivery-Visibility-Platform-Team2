package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RouteRequest {

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;

    /**
     * Optional at creation time so a route can be planned before a driver is
     * assigned. When present, it must identify a logistics operator.
     */
    private Long driverId;

    /**
     * If omitted, the shipment pickup address is used.
     */
    @Size(max = 500, message = "Origin must not exceed 500 characters")
    private String origin;

    /**
     * If omitted, the shipment delivery address is used.
     */
    @Size(max = 500, message = "Destination must not exceed 500 characters")
    private String destination;

    @Size(max = 4000, message = "Waypoints must not exceed 4000 characters")
    private String waypoints;

    @Size(max = 50, message = "Traffic condition must not exceed 50 characters")
    private String trafficCondition;
}
