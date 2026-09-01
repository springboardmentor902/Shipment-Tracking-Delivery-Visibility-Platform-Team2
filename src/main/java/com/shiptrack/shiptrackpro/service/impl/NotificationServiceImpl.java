package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.NotificationCreateRequest;
import com.shiptrack.shiptrackpro.dto.NotificationResponse;
import com.shiptrack.shiptrackpro.entity.Notification;
import com.shiptrack.shiptrackpro.entity.NotificationStatus;
import com.shiptrack.shiptrackpro.entity.NotificationType;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.NotificationRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.service.CurrentUserService;
import com.shiptrack.shiptrackpro.service.NotificationEmailSender;
import com.shiptrack.shiptrackpro.service.NotificationService;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final ShipmentRepository shipmentRepository;
    private final CurrentUserService currentUserService;
    private final ShipmentAccessService shipmentAccessService;
    private final NotificationEmailSender notificationEmailSender;

    @Value("${app.notification.dedupe-minutes:30}")
    private long duplicatePreventionMinutes;

    @Override
    @Transactional
    public Optional<NotificationResponse> send(
            NotificationType type,
            User user,
            Shipment shipment
    ) {
        return sendInternal(type, user, shipment, null, null);
    }

    @Override
    @Transactional
    public Optional<NotificationResponse> send(
            String type,
            User user,
            Shipment shipment
    ) {
        return send(parseType(type), user, shipment);
    }

    @Override
    @Transactional
    public NotificationResponse createForCurrentUser(NotificationCreateRequest request) {
        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + request.getShipmentId()
                ));
        shipmentAccessService.requireCanViewShipment(shipment);

        User currentUser = currentUserService.getRequiredCurrentUser();
        return sendInternal(
                request.getType(),
                currentUser,
                shipment,
                request.getTitle(),
                request.getMessage()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.CONFLICT,
                "An equivalent notification was already sent recently for this shipment"
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getCurrentUserNotifications() {
        User currentUser = currentUserService.getRequiredCurrentUser();
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markCurrentUserNotificationAsRead(Long notificationId) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found with id: " + notificationId
                ));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to update this notification");
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        notification.setStatus(NotificationStatus.READ);
        return mapToResponse(notificationRepository.save(notification));
    }

    private Optional<NotificationResponse> sendInternal(
            NotificationType type,
            User user,
            Shipment shipment,
            String requestedTitle,
            String requestedMessage
    ) {
        if (type == null || user == null || shipment == null) {
            LOGGER.warn("Skipped notification because type, recipient, or shipment was missing");
            return Optional.empty();
        }

        String title = defaultIfBlank(requestedTitle, titleFor(type));
        String message = defaultIfBlank(requestedMessage, messageFor(type, shipment));

        long safeWindow = Math.max(0, duplicatePreventionMinutes);
        LocalDateTime duplicateCutoff = LocalDateTime.now().minusMinutes(safeWindow);
        if (notificationRepository.existsByShipment_IdAndTypeAndMessageAndCreatedAtAfter(
                shipment.getId(), type, message, duplicateCutoff)) {
            LOGGER.info("Suppressed duplicate {} notification for shipment {}", type, shipment.getId());
            return Optional.empty();
        }

        Notification notification = Notification.builder()
                .user(user)
                .shipment(shipment)
                .type(type)
                .title(title)
                .message(message)
                .status(NotificationStatus.PENDING)
                .build();
        notification = notificationRepository.save(notification);

        try {
            notificationEmailSender.send(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception exception) {
            notification.setStatus(NotificationStatus.FAILED);
            LOGGER.warn("Email delivery failed for notification {}", notification.getId(), exception);
        }

        return Optional.of(mapToResponse(notificationRepository.save(notification)));
    }

    private NotificationType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Notification type is required");
        }
        try {
            return NotificationType.valueOf(type.trim()
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_'));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported notification type: " + type);
        }
    }

    private String titleFor(NotificationType type) {
        return switch (type) {
            case SHIPMENT_UPDATE -> "Shipment update";
            case DELAY_WARNING -> "Delivery delay warning";
            case DELIVERY_ALERT -> "Delivery alert";
            case ETA_UPDATE -> "ETA update";
            case MANUAL -> "Shipment notification";
        };
    }

    private String messageFor(NotificationType type, Shipment shipment) {
        String trackingNumber = shipment.getTrackingNumber() == null
                ? "your shipment"
                : "shipment " + shipment.getTrackingNumber();
        return switch (type) {
            case SHIPMENT_UPDATE -> trackingNumber + " is now "
                    + shipment.getStatus().replace('_', ' ').toLowerCase(Locale.ROOT) + ".";
            case DELAY_WARNING -> trackingNumber + " may be delayed. Please check the latest tracking details.";
            case DELIVERY_ALERT -> trackingNumber + " has been delivered. Proof of delivery is available.";
            case ETA_UPDATE -> "The estimated delivery time for " + trackingNumber + " has been updated.";
            case MANUAL -> "There is a new notification for " + trackingNumber + ".";
        };
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .shipmentId(notification.getShipment().getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus().name())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
