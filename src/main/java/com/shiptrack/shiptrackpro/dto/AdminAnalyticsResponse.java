package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class AdminAnalyticsResponse {

    private long totalUsers;
    private long totalShipments;
    private long activeShipments;
    private long pendingPodVerifications;
    private long pendingVerifications;
    private long delayedShipments;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> monthlyShipmentVolume;
}
