package com.shiptrack.shiptrackpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "routes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A shipment may have multiple historical routes. Exactly one is marked
     * current by RouteService when a route is created or replaced.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false, updatable = false)
    private User createdBy;

    @Column(name = "origin_address", nullable = false, length = 500)
    private String origin;

    @Column(name = "destination_address", nullable = false, length = 500)
    private String destination;

    @Column(precision = 10, scale = 7)
    private BigDecimal originLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal originLongitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal destinationLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal destinationLongitude;

    @Column(columnDefinition = "TEXT")
    private String waypoints;

    @Column(precision = 10, scale = 2)
    private BigDecimal distanceKm;

    private Integer estimatedTimeMinutes;

    private Integer actualTimeMinutes;

    @Column(precision = 10, scale = 7)
    private BigDecimal lastKnownLatitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal lastKnownLongitude;

    private LocalDateTime lastLocationUpdatedAt;

    private String trafficCondition;

    @Column(nullable = false)
    private boolean isCurrent;

    @Column(length = 1000)
    private String routeSummary;

    @Column(length = 1000)
    private String selectionReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (trafficCondition == null || trafficCondition.isBlank()) {
            trafficCondition = "NORMAL";
        }

        if (!isCurrent) {
            isCurrent = true;
        }
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
