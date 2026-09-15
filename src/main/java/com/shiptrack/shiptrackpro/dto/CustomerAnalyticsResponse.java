package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.io.Serializable;

@Getter
@Builder
public class CustomerAnalyticsResponse implements Serializable {

    private long totalShipments;
    private long totalShipmentHistoryCount;
    private long activeShipments;
    private long deliveredShipments;
    private long attentionRequired;
    private long totalTrackingEvents;
    private java.time.LocalDateTime lastTrackingUpdate;
    private long pendingVerifications;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> monthlyShipmentVolume;
    private List<NotificationResponse> recentNotifications;
    private List<ShipmentAnalyticsItem> shipmentHistory;
}
