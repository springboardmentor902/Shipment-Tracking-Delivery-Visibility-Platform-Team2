package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.io.Serializable;

@Getter
@Builder
public class BusinessClientAnalyticsResponse implements Serializable {

    private long totalShipmentVolume;
    private long totalShipments;
    private long activeShipments;
    private long deliveredShipments;
    private long failedDeliveries;
    private long onTimeDeliveries;
    private double deliverySuccessRate;
    private long attentionRequired;
    private long atRiskShipments;
    private long totalTrackingEvents;
    private long pendingVerifications;
    private Map<String, Long> statusBreakdown;
    private Map<String, Long> monthlyShipmentVolume;
    private long delayedShipmentCount;
    private double averageDelayDays;
    private Map<String, Long> customerActivity;
    private java.util.List<ShipmentAnalyticsItem> shipmentHistory;
    private java.util.List<ShipmentAnalyticsItem> atRiskShipmentList;
}
