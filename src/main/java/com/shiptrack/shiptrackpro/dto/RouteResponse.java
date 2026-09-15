package com.shiptrack.shiptrackpro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@Builder
public class RouteResponse implements Serializable {

    private Long id;
    private Long shipmentId;
    private String trackingNumber;

    private Long driverId;
    private String driverName;
    private String driverEmail;

    private String origin;
    private String destination;
    private BigDecimal originLatitude;
    private BigDecimal originLongitude;
    private BigDecimal destinationLatitude;
    private BigDecimal destinationLongitude;
    private String waypoints;

    private BigDecimal distanceKm;
    private Integer estimatedTimeMinutes;
    private Integer actualTimeMinutes;
    private BigDecimal lastKnownLatitude;
    private BigDecimal lastKnownLongitude;
    private LocalDateTime lastLocationUpdatedAt;
    private String trafficCondition;
    @JsonProperty("isCurrent")
    private boolean isCurrent;
    private String routeSummary;
    private String selectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
