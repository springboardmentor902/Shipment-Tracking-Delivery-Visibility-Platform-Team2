package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ShipmentResponse {

    private Long id;
    private String trackingNumber;
    private Long createdById;
    private Long assignedOperatorId;

    private String senderName;
    private String senderPhone;
    private String senderAddress;

    private String receiverName;
    private String receiverPhone;
    private String receiverEmail;
    private String receiverAddress;

    private String pickupAddress;
    private String deliveryAddress;

    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double deliveryLatitude;
    private Double deliveryLongitude;

    private String status;
    private String priority;

    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;

    private String cancellationReason;

    private String packageDescription;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    private Integer quantity;
    private Double declaredValue;
    private Boolean fragile;

    private List<PackageResponse> packages;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
