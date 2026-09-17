package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.AdminAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.BusinessClientAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.CustomerAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.NotificationResponse;
import com.shiptrack.shiptrackpro.dto.RouteAnalyticsResponse;
import com.shiptrack.shiptrackpro.dto.RouteResponse;
import com.shiptrack.shiptrackpro.dto.ShipmentAnalyticsItem;
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
import com.shiptrack.shiptrackpro.repository.TrackingEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.Cacheable;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String IN_TRANSIT = "IN_TRANSIT";
    private static final String DELIVERED = "DELIVERED";
    private static final String CANCELLED = "CANCELLED";
    private static final String DELAYED = "DELAYED";

    private final ShipmentRepository shipmentRepository;
    private final NotificationRepository notificationRepository;
    private final ProofOfDeliveryRepository proofOfDeliveryRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final RouteRepository routeRepository;
    private final EtaPredictionRepository etaPredictionRepository;
    private final TrackingEventRepository trackingEventRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "customerAnalytics", key = "#customerId")
    public CustomerAnalyticsResponse getCustomerDashboard(Long customerId) {
        requireCurrentUserOrAdmin(customerId, "CUSTOMER");
        List<Shipment> shipments = shipmentRepository.findByCreatedBy_Id(customerId);

        return CustomerAnalyticsResponse.builder()
                .totalShipments(shipments.size())
                .totalShipmentHistoryCount(shipments.size())
                .activeShipments(countActiveShipments(shipments))
                .deliveredShipments(countStatus(shipments, DELIVERED))
                .cancelledShipments(countStatus(shipments, CANCELLED))
                .attentionRequired(countAttentionRequired(shipments))
                .totalTrackingEvents(totalTrackingEvents(shipments))
                .lastTrackingUpdate(lastTrackingUpdate(shipments))
                .pendingVerifications(0)
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .recentNotifications(notificationRepository
                        .findByUser_IdOrderByCreatedAtDesc(customerId)
                        .stream()
                        .limit(10)
                        .map(this::toNotificationResponse)
                        .toList())
                .shipmentHistory(shipmentHistory(shipments))
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "businessAnalytics", key = "#clientId")
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
                .deliveredShipments(countStatus(shipments, DELIVERED))
                .failedDeliveries(countFailed(shipments))
                .onTimeDeliveries(countOnTime(shipments))
                .deliverySuccessRate(percentage(countStatus(shipments, DELIVERED), shipments.size()))
                .attentionRequired(countAttentionRequired(shipments))
                .atRiskShipments(countAttentionRequired(shipments))
                .totalTrackingEvents(totalTrackingEvents(shipments))
                .pendingVerifications(0)
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .delayedShipmentCount(delayed.size())
                .averageDelayDays(averageDelayDays(delayed))
                .customerActivity(customerActivity(shipments))
                .shipmentHistory(shipmentHistory(shipments))
                .atRiskShipmentList(shipmentHistory(shipments.stream()
                        .filter(this::requiresAttention)
                        .toList()))
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "adminAnalytics", key = "'platform'")
    public AdminAnalyticsResponse getAdminDashboard() {
        requireAdmin();
        List<Shipment> shipments = shipmentRepository.findAll();
        long pendingPodVerifications = proofOfDeliveryRepository
                .findByVerificationStatusOrderByDeliveredAtAsc(
                        ProofOfDeliveryVerificationStatus.PENDING)
                .size();

        long delivered = countStatus(shipments, DELIVERED);
        long onTime = countOnTime(shipments);
        return AdminAnalyticsResponse.builder()
                .totalUsers(userRepository.count())
                .activeUsers(userRepository.findAll().stream()
                        .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                        .count())
                .totalShipments(shipments.size())
                .deliveredShipments(delivered)
                .failedDeliveries(countFailed(shipments))
                .onTimeDeliveries(onTime)
                .onTimeDeliveryRate(percentage(onTime, delivered))
                .totalTrackingEvents(totalTrackingEvents(shipments))
                .lastTrackingUpdate(lastTrackingUpdate(shipments))
                .activeShipments(countActiveShipments(shipments))
                .attentionRequired(countAttentionRequired(shipments))
                .pendingPodVerifications(pendingPodVerifications)
                .pendingVerifications(pendingPodVerifications)
                .delayedShipments(countStatus(shipments, DELAYED))
                .statusBreakdown(statusBreakdown(shipments))
                .monthlyShipmentVolume(monthlyVolume(shipments))
                .userRoleBreakdown(userRoleBreakdown())
                .routeAnalytics(routeAnalytics())
                .systemStatus("OPERATIONAL")
                .generatedAt(LocalDateTime.now())
                .availableReports(List.of("SHIPMENTS", "DELIVERIES", "ROUTES", "DELAYS"))
                .build();
    }

    private long totalTrackingEvents(List<Shipment> shipments) {
        List<Long> ids = shipmentIds(shipments);
        return ids.isEmpty() ? 0 : trackingEventRepository.countByShipment_IdIn(ids);
    }

    private LocalDateTime lastTrackingUpdate(List<Shipment> shipments) {
        List<Long> ids = shipmentIds(shipments);
        return ids.isEmpty() ? null : trackingEventRepository
                .findFirstByShipment_IdInOrderByEventTimestampDesc(ids)
                .map(event -> event.getEventTimestamp())
                .orElse(null);
    }

    private List<Long> shipmentIds(List<Shipment> shipments) {
        return shipments.stream().map(Shipment::getId).toList();
    }

    private long countFailed(List<Shipment> shipments) {
        return shipments.stream()
                .filter(shipment -> "FAILED".equalsIgnoreCase(shipment.getStatus())
                        || "FAILED_DELIVERY".equalsIgnoreCase(shipment.getStatus()))
                .count();
    }

    private long countOnTime(List<Shipment> shipments) {
        return shipments.stream()
                .filter(shipment -> DELIVERED.equalsIgnoreCase(shipment.getStatus()))
                .filter(shipment -> shipment.getActualDeliveryDate() != null
                        && shipment.getEstimatedDeliveryDate() != null
                        && !shipment.getActualDeliveryDate().isAfter(
                        shipment.getEstimatedDeliveryDate()))
                .count();
    }

    private double percentage(long value, long total) {
        return total == 0 ? 0 : round(value * 100.0 / total);
    }

    private Map<String, Long> customerActivity(List<Shipment> shipments) {
        return shipments.stream()
                .filter(shipment -> shipment.getReceiverName() != null)
                .collect(Collectors.groupingBy(
                        Shipment::getReceiverName,
                        TreeMap::new,
                        Collectors.counting()));
    }

    private Map<String, Long> userRoleBreakdown() {
        return userRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        User::getRole,
                        TreeMap::new,
                        Collectors.counting()));
    }

    private List<ShipmentAnalyticsItem> shipmentHistory(List<Shipment> shipments) {
        return shipments.stream()
                .sorted(Comparator.comparing(
                        Shipment::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(shipment -> ShipmentAnalyticsItem.builder()
                        .id(shipment.getId())
                        .trackingNumber(shipment.getTrackingNumber())
                        .status(shipment.getStatus())
                        .receiverName(shipment.getReceiverName())
                        .pickupAddress(shipment.getPickupAddress())
                        .deliveryAddress(shipment.getDeliveryAddress())
                        .createdAt(shipment.getCreatedAt())
                        .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                        .actualDeliveryDate(shipment.getActualDeliveryDate())
                        .build())
                .toList();
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
        if (!currentUserService.hasAnyRole(currentUser, "ADMINISTRATOR", "SUB_ADMINISTRATOR", "SUPPORT_AGENT")) {
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
                .lastKnownLatitude(route.getLastKnownLatitude())
                .lastKnownLongitude(route.getLastKnownLongitude())
                .lastLocationUpdatedAt(route.getLastLocationUpdatedAt())
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
