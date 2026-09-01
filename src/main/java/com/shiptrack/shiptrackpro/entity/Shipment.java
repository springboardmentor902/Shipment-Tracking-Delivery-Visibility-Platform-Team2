package com.shiptrack.shiptrackpro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /** The customer or business client that created the shipment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /** The logistics operator currently responsible for the shipment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_operator_id")
    private User assignedOperator;

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

    @Builder.Default
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentPackage> packages = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = "CREATED";
        }

        if (priority == null) {
            priority = "STANDARD";
        }

    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void replacePackages(List<ShipmentPackage> newPackages) {
        packages.clear();
        if (newPackages == null) {
            return;
        }
        newPackages.forEach(shipmentPackage -> {
            shipmentPackage.setShipment(this);
            packages.add(shipmentPackage);
        });
    }
}
