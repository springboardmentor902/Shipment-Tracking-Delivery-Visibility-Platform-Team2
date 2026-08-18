package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShipmentRequest {

    @NotBlank
    private String senderName;

    private String senderPhone;
    private String senderAddress;

    @NotBlank
    private String receiverName;

    private String receiverPhone;
    private String receiverEmail;
    private String receiverAddress;

    @NotBlank
    private String pickupAddress;

    @NotBlank
    private String deliveryAddress;

    private String priority;

    private String packageDescription;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    private Integer quantity;
    private Double declaredValue;
    private Boolean fragile;
}