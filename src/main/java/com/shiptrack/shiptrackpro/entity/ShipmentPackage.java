package com.shiptrack.shiptrackpro.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A physical package belonging to one shipment. A shipment may contain many
 * packages, each with its own dimensions and declared value.
 */
@Entity
@Table(name = "packages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false)
    private String description;

    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    @Column(nullable = false)
    private Integer quantity;

    private BigDecimal declaredValue;

    @Column(nullable = false)
    private Boolean fragile;

    @PrePersist
    protected void onCreate() {
        if (quantity == null) {
            quantity = 1;
        }
        if (fragile == null) {
            fragile = false;
        }
    }
}
