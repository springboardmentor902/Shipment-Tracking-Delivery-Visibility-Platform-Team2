package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class ShipmentAnalyticsItem implements Serializable {
    private Long id;
    private String trackingNumber;
    private String status;
    private String receiverName;
    private String pickupAddress;
    private String deliveryAddress;
    private LocalDateTime createdAt;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
}
