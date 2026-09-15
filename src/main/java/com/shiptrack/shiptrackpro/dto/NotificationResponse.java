package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@Builder
public class NotificationResponse implements Serializable {

    private Long id;
    private Long shipmentId;
    private String type;
    private String title;
    private String message;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
