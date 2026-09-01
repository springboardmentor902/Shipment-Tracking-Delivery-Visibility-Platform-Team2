package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RouteResponse {

    private Long id;
    private Long shipmentId;
    private String trackingNumber;

    private Long driverId;
    private String driverName;
    private String driverEmail;

    private String origin;
    private String destination;
    private String waypoints;

    private BigDecimal distanceKm;
    private Integer estimatedTimeMinutes;
    private Integer actualTimeMinutes;
    private String trafficCondition;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
