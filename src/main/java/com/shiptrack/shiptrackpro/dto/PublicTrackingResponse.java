package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PublicTrackingResponse {

    private String trackingNumber;
    private String status;
    private LocalDateTime estimatedDeliveryDate;
    private LocalDateTime actualDeliveryDate;
    private LocalDateTime lastUpdatedAt;
    private List<Event> events;

    @Getter
    @Builder
    public static class Event {
        private String status;
        private String location;
        private LocalDateTime eventTimestamp;
    }
}
