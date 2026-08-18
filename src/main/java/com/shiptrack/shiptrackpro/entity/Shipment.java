package com.shiptrack.shiptrackpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

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

    @Column(nullable = false)
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

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = "CREATED";
        }

        if (priority == null) {
            priority = "STANDARD";
        }

        if (quantity == null) {
            quantity = 1;
        }

        if (fragile == null) {
            fragile = false;
        }
    }
}