package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.entity.Notification;
import com.shiptrack.shiptrackpro.entity.NotificationStatus;
import com.shiptrack.shiptrackpro.repository.NotificationRepository;
import com.shiptrack.shiptrackpro.service.NotificationEmailSender;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Delivers email without delaying the API response that created the notification. */
@Service
@RequiredArgsConstructor
public class NotificationEmailDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEmailDeliveryService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationEmailSender notificationEmailSender;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return;
        }

        try {
            notificationEmailSender.send(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception exception) {
            notification.setStatus(NotificationStatus.FAILED);
            LOGGER.warn("Email delivery failed for notification {}", notificationId, exception);
        }

        notificationRepository.save(notification);
    }
}
