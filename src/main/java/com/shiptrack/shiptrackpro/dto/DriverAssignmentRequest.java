package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverAssignmentRequest {

    @NotNull(message = "Driver id is required")
    private Long driverId;
}
