package com.shiptrack.shiptrackpro.dto;

import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class VerifyProofOfDeliveryRequest {

    @NotNull(message = "Verification status is required")
    private ProofOfDeliveryVerificationStatus verificationStatus;

    @Size(max = 500)
    private String verificationNotes;
}
