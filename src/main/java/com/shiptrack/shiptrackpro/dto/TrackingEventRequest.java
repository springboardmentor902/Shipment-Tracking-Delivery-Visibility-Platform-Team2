package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrackingEventRequest {

    @NotBlank(message = "Tracking status is required")
    @Size(max = 50, message = "Tracking status must not exceed 50 characters")
    private String status;

    @Size(max = 500, message = "Location must not exceed 500 characters")
    private String location;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /** Defaults to the server time when omitted. */
    private LocalDateTime eventTimestamp;
}
