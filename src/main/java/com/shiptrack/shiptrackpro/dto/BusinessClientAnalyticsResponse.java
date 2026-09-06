package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class BusinessClientAnalyticsResponse {

    private long totalShipmentVolume;
    private long totalShipments;
    private long activeShipments;
    private long pendingVerifications;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> monthlyShipmentVolume;
    private long delayedShipmentCount;
    private double averageDelayDays;
}
