package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TrackingEventResponse {

    private Long id;
    private Long shipmentId;
    private String trackingNumber;

    private String status;
    private String location;
    private Double latitude;
    private Double longitude;
    private String notes;
    private LocalDateTime eventTimestamp;

    private Long updatedById;
    private String updatedByName;
}
