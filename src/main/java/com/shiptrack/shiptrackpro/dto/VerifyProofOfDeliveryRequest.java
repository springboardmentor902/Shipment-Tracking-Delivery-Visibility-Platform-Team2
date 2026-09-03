package com.shiptrack.shiptrackpro.dto;

import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyProofOfDeliveryRequest {

    @NotNull(message = "Verification status is required")
    private ProofOfDeliveryVerificationStatus verificationStatus;
}
