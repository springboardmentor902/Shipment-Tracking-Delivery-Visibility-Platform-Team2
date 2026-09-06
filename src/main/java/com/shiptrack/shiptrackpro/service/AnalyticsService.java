package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.AdminAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.BusinessClientAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.CustomerAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.NotificationResponse;
import com.shiptrack.shiptrackpro.entity.Notification;
import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.NotificationRepository;
import com.shiptrack.shiptrackpro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String IN_TRANSIT = "IN_TRANSIT";
    private static final String DELIVERED = "DELIVERED";
    private static final String DELAYED = "DELAYED";

    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerDashboard(Long customerId) {
        requireCurrentUserOrAdmin(customerId, "CUSTOMER");
        List<Shipment> shipments = shipmentRepository.findByCreatedBy_Id(customerId);

        return CustomerAnalyticsResponse.builder()
                .totalShipments(shipments.size())
                .totalShipmentHistoryCount(shipments.size())
                .activeShipments(countStatus(shipments, IN_TRANSIT))
                .pendingVerifications(0)
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .recentNotifications(notificationRepository
                        .findByUser_IdOrderByCreatedAtDesc(customerId)
                        .stream()
                        .limit(10)
                        .map(this::toNotificationResponse)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public BusinessClientAnalyticsResponse getBusinessClientDashboard(Long clientId) {
        requireCurrentUserOrAdmin(clientId, "BUSINESS_CLIENT");
        List<Shipment> shipments = shipmentRepository.findByCreatedBy_Id(clientId);
        List<Shipment> delayed = shipments.stream()
                .filter(shipment -> DELAYED.equalsIgnoreCase(shipment.getStatus()))
                .toList();

        return BusinessClientAnalyticsResponse.builder()
                .totalShipmentVolume(shipments.size())
                .totalShipments(shipments.size())
                .activeShipments(countStatus(shipments, IN_TRANSIT))
                .pendingVerifications(0)
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .delayedShipmentCount(delayed.size())
                .averageDelayDays(averageDelayDays(delayed))
                .build();
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAdminDashboard() {
        requireAdmin();
        List<Shipment> shipments = shipmentRepository.findAll();
        long pendingPodVerifications = proofOfDeliveryRepository
                .findByVerificationStatusOrderByDeliveredAtAsc(
                        ProofOfDeliveryVerificationStatus.PENDING)
                .size();

        return AdminAnalyticsResponse.builder()
                .totalUsers(userRepository.count())
                .totalShipments(shipments.size())
                .activeShipments(countStatus(shipments, IN_TRANSIT))
                .pendingPodVerifications(pendingPodVerifications)
                .pendingVerifications(pendingPodVerifications)
                .delayedShipments(countStatus(shipments, DELAYED))
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .build();
    }

    private void requireCurrentUserOrAdmin(Long userId, String expectedRole) {
        User currentUser = currentUserService.getRequiredCurrentUser();
        boolean isAdmin = currentUserService.hasRole(currentUser, "ADMINISTRATOR");
        boolean isExpectedRole = currentUserService.hasRole(currentUser, expectedRole);
        if ((!isAdmin && !isExpectedRole) || (!isAdmin && !currentUser.getId().equals(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to view this dashboard");
        }
    }

    private void requireAdmin() {
        User currentUser = currentUserService.getRequiredCurrentUser();
        if (!currentUserService.hasAnyRole(currentUser, "ADMINISTRATOR", "SUPPORT_AGENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Administrator access is required");
        }
    }

    private long countStatus(List<Shipment> shipments, String status) {
        return shipments.stream()
                .filter(shipment -> status.equalsIgnoreCase(shipment.getStatus()))
                .count();
    }

    private Map<String, Long> statusBreakdown(List<Shipment> shipments) {
        Map<String, Long> result = new LinkedHashMap<>();
        shipments.stream()
                .map(Shipment::getStatus)
                .filter(status -> status != null && !status.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .forEach(status -> result.put(status, countStatus(shipments, status)));
        return result;
    }

    private Map<String, Long> monthlyVolume(List<Shipment> shipments) {
        return shipments.stream()
                .filter(shipment -> shipment.getCreatedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        shipment -> YearMonth.from(shipment.getCreatedAt()).toString(),
                        TreeMap::new,
                        java.util.stream.Collectors.counting()));
    }

    private double averageDelayDays(List<Shipment> delayedShipments) {
        List<Long> delayDays = delayedShipments.stream()
                .filter(shipment -> shipment.getEstimatedDeliveryDate() != null)
                .map(shipment -> {
                    LocalDateTime end = shipment.getActualDeliveryDate() == null
                            ? LocalDateTime.now()
                            : shipment.getActualDeliveryDate();
                    return Math.max(0, ChronoUnit.DAYS.between(
                            shipment.getEstimatedDeliveryDate(), end));
                })
                .toList();
        return delayDays.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
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
