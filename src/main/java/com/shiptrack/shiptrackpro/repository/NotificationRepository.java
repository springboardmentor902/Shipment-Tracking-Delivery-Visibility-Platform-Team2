package com.shiptrack.shiptrackpro.repository;

import com.shiptrack.shiptrackpro.entity.Notification;
import com.shiptrack.shiptrackpro.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);

    boolean existsByShipment_IdAndTypeAndMessageAndCreatedAtAfter(
            Long shipmentId,
            NotificationType type,
            String message,
            LocalDateTime sentAfter
    );
}
