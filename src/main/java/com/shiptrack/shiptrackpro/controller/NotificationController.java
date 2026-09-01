package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.NotificationCreateRequest;
import com.shiptrack.shiptrackpro.dto.NotificationResponse;
import com.shiptrack.shiptrackpro.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications() {
        return ResponseEntity.ok(notificationService.getCurrentUserNotifications());
    }

    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markCurrentUserNotificationAsRead(id));
    }

    /** The singular path is retained because it is part of the mentor API contract. */
    @PostMapping("/api/notification")
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody NotificationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createForCurrentUser(request));
    }
}
