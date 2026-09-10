package com.shiptrack.shiptrackpro.dto;

import com.shiptrack.shiptrackpro.entity.NotificationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationCreateRequest {

    @NotNull(message = "Shipment id is required")
    private Long shipmentId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @Size(max = 160, message = "Notification title must be at most 160 characters")
    private String title;

    @Size(max = 4000, message = "Notification message must be at most 4000 characters")
    private String message;
}
