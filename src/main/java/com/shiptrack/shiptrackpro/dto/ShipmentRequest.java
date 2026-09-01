package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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

    /** New package section; every row is persisted in the packages table. */
    @Valid
    private List<PackageRequest> packages = new ArrayList<>();

    /*
     * Compatibility fields for the original single-package form. They are
     * converted to one Package record only when the new packages array is
     * absent. New clients should always use packages.
     */
    private String packageDescription;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;
    private Integer quantity;
    private Double declaredValue;
    private Boolean fragile;
}
