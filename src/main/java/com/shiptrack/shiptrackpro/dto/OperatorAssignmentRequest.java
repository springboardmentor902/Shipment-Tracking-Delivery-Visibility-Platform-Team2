package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OperatorAssignmentRequest {
    @NotNull(message = "operatorId is required")
    private Long operatorId;
}
