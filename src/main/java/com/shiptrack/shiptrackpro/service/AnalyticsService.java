package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.AdminAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.BusinessClientAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.CustomerAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.NotificationResponse;
import com.shiptrack.shiptrackpro.dto.RouteAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.RouteResponse;
import com.shiptrack.shiptrackpro.entity.Notification;
import com.shiptrack.shiptrackpro.entity.EtaPrediction;
import com.shiptrack.shiptrackpro.entity.ProofOfDeliveryVerificationStatus;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.Route;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.NotificationRepository;
import com.shiptrack.shiptrackpro.repository.EtaPredictionRepository;
import com.shiptrack.shiptrackpro.repository.ProofOfDeliveryRepository;
import com.shiptrack.shiptrackpro.repository.RouteRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;

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
    private final RouteRepository routeRepository;
    private final EtaPredictionRepository etaPredictionRepository;

    @Transactional(readOnly = true)
    public CustomerAnalyticsResponse getCustomerDashboard(Long customerId) {
        requireCurrentUserOrAdmin(customerId, "CUSTOMER");
        List<Shipment> shipments = shipmentRepository.findByCreatedBy_Id(customerId);

        return CustomerAnalyticsResponse.builder()
                .totalShipments(shipments.size())
                .totalShipmentHistoryCount(shipments.size())
                .activeShipments(countActiveShipments(shipments))
                .attentionRequired(countAttentionRequired(shipments))
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
                .activeShipments(countActiveShipments(shipments))
                .attentionRequired(countAttentionRequired(shipments))
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
                .activeShipments(countActiveShipments(shipments))
                .attentionRequired(countAttentionRequired(shipments))
                .pendingPodVerifications(pendingPodVerifications)
                .pendingVerifications(pendingPodVerifications)
                .delayedShipments(countStatus(shipments, DELAYED))
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .routeAnalytics(routeAnalytics())
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

    /** A shipment is active from creation until it reaches a final outcome. */
    private long countActiveShipments(List<Shipment> shipments) {
        return shipments.stream()
                .map(Shipment::getStatus)
                .filter(status -> status != null)
                .map(String::toUpperCase)
                .filter(status -> !DELIVERED.equals(status)
                        && !"FAILED".equals(status)
                        && !"FAILED_DELIVERY".equals(status)
                        && !"CANCELLED".equals(status))
                .count();
    }

    private long countAttentionRequired(List<Shipment> shipments) {
        return shipments.stream()
                .filter(this::requiresAttention)
                .count();
    }

    private boolean requiresAttention(Shipment shipment) {
        if (DELAYED.equalsIgnoreCase(shipment.getStatus())) {
            return true;
        }
        return etaPredictionRepository.findByShipment_Id(shipment.getId())
                .map(EtaPrediction::getDelayRiskScore)
                .map(score -> score.compareTo(BigDecimal.valueOf(7)) >= 0)
                .orElse(false);
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

    private RouteAnalyticsResponse routeAnalytics() {
        List<Route> routes = routeRepository.findAll();
        List<Route> routesWithDistance = routes.stream()
                .filter(route -> route.getDistanceKm() != null)
                .toList();
        List<Route> routesWithActualTime = routes.stream()
                .filter(route -> route.getEstimatedTimeMinutes() != null
                        && route.getEstimatedTimeMinutes() > 0
                        && actualDurationMinutes(route) != null)
                .toList();

        double averageDistance = routesWithDistance.stream()
                .map(Route::getDistanceKm)
                .mapToDouble(distance -> distance.doubleValue())
                .average()
                .orElse(0);

        double accuracy = routesWithActualTime.stream()
                .mapToDouble(route -> {
                    double difference = Math.abs(actualDurationMinutes(route)
                            - route.getEstimatedTimeMinutes());
                    return Math.max(0, 100 - (difference / route.getEstimatedTimeMinutes()) * 100);
                })
                .average()
                .orElse(0);

        Comparator<Route> performanceComparator = routesWithActualTime.isEmpty()
                ? Comparator.comparing(route -> route.getEstimatedTimeMinutes() == null
                        ? Integer.MAX_VALUE : route.getEstimatedTimeMinutes())
                : Comparator.comparingDouble(this::estimateDifferencePercent);
        List<Route> performanceRoutes = routesWithActualTime.isEmpty() ? routes : routesWithActualTime;

        Route best = performanceRoutes.stream().min(performanceComparator).orElse(null);
        Route worst = performanceRoutes.stream().max(performanceComparator).orElse(null);

        return RouteAnalyticsResponse.builder()
                .totalRoutes(routes.size())
                .averageRouteDistanceKm(round(averageDistance))
                .timeEstimateAccuracyPercent(round(accuracy))
                .routesWithActualTime(routesWithActualTime.size())
                .bestPerformingRoute(best == null ? null : toRouteResponse(best))
                .worstPerformingRoute(worst == null ? null : toRouteResponse(worst))
                .build();
    }

    private double estimateDifferencePercent(Route route) {
        if (route.getEstimatedTimeMinutes() == null || route.getEstimatedTimeMinutes() <= 0
                || actualDurationMinutes(route) == null) {
            return Double.MAX_VALUE;
        }
        return Math.abs(actualDurationMinutes(route) - route.getEstimatedTimeMinutes())
                * 100.0 / route.getEstimatedTimeMinutes();
    }

    private Integer actualDurationMinutes(Route route) {
        if (route.getActualTimeMinutes() != null) {
            return route.getActualTimeMinutes();
        }
        Shipment shipment = route.getShipment();
        if (shipment.getActualDeliveryDate() == null || route.getCreatedAt() == null) {
            return null;
        }
        return Math.max(0, (int) ChronoUnit.MINUTES.between(
                route.getCreatedAt(), shipment.getActualDeliveryDate()));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private RouteResponse toRouteResponse(Route route) {
        User driver = route.getDriver();
        Shipment shipment = route.getShipment();
        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .driverId(driver == null ? null : driver.getId())
                .driverName(driver == null ? null : driver.getFullName())
                .driverEmail(driver == null ? null : driver.getEmail())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .waypoints(route.getWaypoints())
                .distanceKm(route.getDistanceKm())
                .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                .actualTimeMinutes(actualDurationMinutes(route))
                .trafficCondition(route.getTrafficCondition())
                .isCurrent(route.isCurrent())
                .routeSummary(route.getRouteSummary())
                .selectionReason(route.getSelectionReason())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
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
