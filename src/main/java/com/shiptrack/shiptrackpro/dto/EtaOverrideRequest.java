package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EtaOverrideRequest {

    @NotNull(message = "Predicted arrival date and time are required")
    @Future(message = "Predicted arrival must be in the future")
    private LocalDateTime predictedDeliveryTime;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
