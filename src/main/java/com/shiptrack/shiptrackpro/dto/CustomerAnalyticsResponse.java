package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CustomerAnalyticsResponse {

    private long totalShipments;
    private long totalShipmentHistoryCount;
    private long activeShipments;
    private long attentionRequired;
    private long pendingVerifications;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> monthlyShipmentVolume;
    private List<NotificationResponse> recentNotifications;
}
