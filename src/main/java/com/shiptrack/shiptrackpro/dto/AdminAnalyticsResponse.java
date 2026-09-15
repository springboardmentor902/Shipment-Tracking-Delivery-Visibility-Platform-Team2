package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.io.Serializable;

@Getter
@Builder
public class AdminAnalyticsResponse implements Serializable {

    private long totalUsers;
    private long activeUsers;
    private long totalShipments;
    private long deliveredShipments;
    private long failedDeliveries;
    private long onTimeDeliveries;
    private double onTimeDeliveryRate;
    private long totalTrackingEvents;
    private java.time.LocalDateTime lastTrackingUpdate;
    private long activeShipments;
    private long attentionRequired;
    private long pendingPodVerifications;
    private long pendingVerifications;
    private long delayedShipments;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> monthlyShipmentVolume;
    private Map<String, Long> userRoleBreakdown;
    private RouteAnalyticsResponse routeAnalytics;
    private String systemStatus;
    private java.time.LocalDateTime generatedAt;
    private java.util.List<String> availableReports;
}
