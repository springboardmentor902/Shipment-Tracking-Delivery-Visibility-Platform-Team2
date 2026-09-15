package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LocationUpdateResponse {
    private Long routeId;
    private Long shipmentId;
    private String trackingNumber;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String location;
    private LocalDateTime recordedAt;
}
