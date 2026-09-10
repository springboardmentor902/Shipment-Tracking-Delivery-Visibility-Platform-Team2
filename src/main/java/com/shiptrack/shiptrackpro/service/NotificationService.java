package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.NotificationCreateRequest;
import com.shiptrack.shiptrackpro.dto.NotificationResponse;
import com.shiptrack.shiptrackpro.entity.NotificationType;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;

import java.util.List;
import java.util.Optional;

public interface NotificationService {

    /**
     * Sends a templated notification unless an equivalent one was sent within
     * the configured duplicate-prevention window.
     */
    Optional<NotificationResponse> send(
            NotificationType type,
            User user,
            Shipment shipment
    );

    /** Convenience overload for tracking and ETA modules that provide a type string. */
    Optional<NotificationResponse> send(
            String type,
            User user,
            Shipment shipment
    );

    NotificationResponse createForCurrentUser(NotificationCreateRequest request);

    List<NotificationResponse> getCurrentUserNotifications();

    NotificationResponse markCurrentUserNotificationAsRead(Long notificationId);
}
