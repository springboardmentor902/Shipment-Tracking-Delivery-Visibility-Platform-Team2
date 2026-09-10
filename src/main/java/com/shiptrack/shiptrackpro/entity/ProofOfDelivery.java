package com.shiptrack.shiptrackpro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "proof_of_delivery")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(name = "signature_url", length = 512)
    private String signatureUrl;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "delivered_to_name", nullable = false, length = 160)
    private String deliveredToName;

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private ProofOfDeliveryVerificationStatus verificationStatus;

    @Column(name = "delivered_at", nullable = false)
    private LocalDateTime deliveredAt;

    @PrePersist
    void initializeDefaults() {
        if (verificationStatus == null) {
            verificationStatus = ProofOfDeliveryVerificationStatus.PENDING;
        }
        if (deliveredAt == null) {
            deliveredAt = LocalDateTime.now();
        }
    }
}
