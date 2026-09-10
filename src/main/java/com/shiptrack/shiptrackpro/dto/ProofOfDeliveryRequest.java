package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProofOfDeliveryRequest {

    private MultipartFile signature;

    private MultipartFile photo;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 160, message = "Recipient name must be at most 160 characters")
    private String deliveredToName;

    @Size(max = 4000, message = "Delivery notes must be at most 4000 characters")
    private String deliveryNotes;
}
