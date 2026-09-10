package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProofOfDeliveryResponse {

    private Long id;
    private Long shipmentId;
    private Long verifiedById;
    private String signatureUrl;
    private String photoUrl;
    private String deliveredToName;
    private String deliveryNotes;
    private String verificationStatus;
    private LocalDateTime deliveredAt;
}
