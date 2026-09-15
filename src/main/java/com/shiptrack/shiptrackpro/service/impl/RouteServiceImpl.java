package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.DriverAssignmentRequest;
import com.shiptrack.shiptrackpro.dto.DriverLocationRequest;
import com.shiptrack.shiptrackpro.dto.LocationUpdateResponse;
import com.shiptrack.shiptrackpro.dto.RouteRequest;
import com.shiptrack.shiptrackpro.dto.RouteResponse;
import com.shiptrack.shiptrackpro.dto.TrackingEventRequest;
import com.shiptrack.shiptrackpro.entity.Route;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.RouteRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import com.shiptrack.shiptrackpro.service.RouteService;
import com.shiptrack.shiptrackpro.service.RouteOptimizationService;
import com.shiptrack.shiptrackpro.service.EtaPredictionService;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import com.shiptrack.shiptrackpro.service.TrackingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final RouteOptimizationService routeOptimizationService;
    private final ShipmentAccessService shipmentAccessService;
    private final EtaPredictionService etaPredictionService;
    private final TrackingEventService trackingEventService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"customerAnalytics", "businessAnalytics", "adminAnalytics"}, allEntries = true)
    public RouteResponse createRoute(RouteRequest request) {
        Authentication authentication = requireRouteManager();

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> notFound("Shipment not found with id: " + request.getShipmentId()));

        assignCurrentOperatorWhenUnassigned(shipment, authentication);
        requireAssignedOperatorOrAdministrator(shipment, authentication);

        String origin = valueOrFallback(request.getOrigin(), shipment.getPickupAddress());
        String destination = valueOrFallback(request.getDestination(), shipment.getDeliveryAddress());

        if (origin == null || destination == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Origin and destination are required. Provide them in the route request "
                            + "or ensure the shipment has pickup and delivery addresses."
            );
        }

        User driver = request.getDriverId() == null
                ? null
                : findLogisticsOperator(request.getDriverId());

        RouteOptimizationService.RouteOptimizationResult optimization =
                routeOptimizationService.optimize(origin, destination);
        var selectedAlternative = optimization.selectedAlternative();
        var originCoordinates = optimization.originCoordinates();
        var destinationCoordinates = optimization.destinationCoordinates();

        if (originCoordinates != null) {
            shipment.setPickupLatitude(originCoordinates.latitude());
            shipment.setPickupLongitude(originCoordinates.longitude());
        }
        if (destinationCoordinates != null) {
            shipment.setDeliveryLatitude(destinationCoordinates.latitude());
            shipment.setDeliveryLongitude(destinationCoordinates.longitude());
        }

        // Rerouting retains the old record for the shipment history while the
        // new route becomes the single active route.
        routeRepository.markCurrentRoutesAsHistorical(shipment.getId());

        Route route = Route.builder()
                .shipment(shipment)
                .driver(driver)
                .createdBy(findCurrentUser(authentication))
                .origin(origin)
                .destination(destination)
                .originLatitude(decimal(originCoordinates == null ? null : originCoordinates.latitude()))
                .originLongitude(decimal(originCoordinates == null ? null : originCoordinates.longitude()))
                .destinationLatitude(decimal(destinationCoordinates == null ? null : destinationCoordinates.latitude()))
                .destinationLongitude(decimal(destinationCoordinates == null ? null : destinationCoordinates.longitude()))
                .waypoints(blankToNull(request.getWaypoints()))
                .trafficCondition(valueOrFallback(request.getTrafficCondition(),
                        selectedAlternative == null ? "NORMAL" : "LIVE_TRAFFIC"))
                .distanceKm(selectedAlternative == null ? null : selectedAlternative.distanceKm())
                .estimatedTimeMinutes(selectedAlternative == null
                        ? null
                        : selectedAlternative.trafficAdjustedDurationMinutes())
                .routeSummary(selectedAlternative == null
                        ? origin + " to " + destination
                        : selectedAlternative.routeSummary())
                .selectionReason(optimization.selectionReason())
                .isCurrent(true)
                .build();

        Route savedRoute = routeRepository.save(route);
        etaPredictionService.recalculateAfterTrackingEvent(shipment.getId());

        return toResponse(savedRoute);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"customerAnalytics", "businessAnalytics", "adminAnalytics"}, allEntries = true)
    public RouteResponse assignDriver(
            Long shipmentId,
            DriverAssignmentRequest request
    ) {
        Authentication authentication = requireRouteManager();

        Route route = findRoute(shipmentId);
        requireAssignedOperatorOrAdministrator(route.getShipment(), authentication);
        route.setDriver(findLogisticsOperator(request.getDriverId()));

        return toResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse getRouteForShipment(Long shipmentId) {
        Route route = findRoute(shipmentId);
        shipmentAccessService.requireCanViewShipment(route.getShipment());
        return toResponse(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteResponse> getRouteHistoryForShipment(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> notFound("Shipment not found with id: " + shipmentId));
        shipmentAccessService.requireCanViewShipment(shipment);
        return routeRepository.findByShipment_IdOrderByCreatedAtDesc(shipmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"customerAnalytics", "businessAnalytics", "adminAnalytics"}, allEntries = true)
    public LocationUpdateResponse updateLocation(
            Long routeId,
            DriverLocationRequest request
    ) {
        Authentication authentication = requireRouteManager();
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> notFound("Route not found with id: " + routeId));
        requireAssignedOperatorOrAdministrator(route.getShipment(), authentication);

        LocalDateTime recordedAt = LocalDateTime.now();
        route.setLastKnownLatitude(request.getLatitude());
        route.setLastKnownLongitude(request.getLongitude());
        route.setLastLocationUpdatedAt(recordedAt);
        routeRepository.save(route);

        TrackingEventRequest trackingRequest = new TrackingEventRequest();
        trackingRequest.setStatus(route.getShipment().getStatus());
        trackingRequest.setLocation(blankToNull(request.getLocation()));
        trackingRequest.setLatitude(request.getLatitude().doubleValue());
        trackingRequest.setLongitude(request.getLongitude().doubleValue());
        trackingRequest.setNotes("Driver location updated");
        trackingRequest.setEventTimestamp(recordedAt);
        trackingEventService.addTrackingEvent(route.getShipment().getId(), trackingRequest);

        LocationUpdateResponse response = LocationUpdateResponse.builder()
                .routeId(route.getId())
                .shipmentId(route.getShipment().getId())
                .trackingNumber(route.getShipment().getTrackingNumber())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(blankToNull(request.getLocation()))
                .recordedAt(recordedAt)
                .build();
        messagingTemplate.convertAndSend(
                "/topic/shipments/" + route.getShipment().getId() + "/location",
                response);
        return response;
    }

    private Route findRoute(Long shipmentId) {
        return routeRepository.findByShipment_IdAndIsCurrentTrue(shipmentId)
                .orElseThrow(() -> notFound(
                        "Route not found for shipment id: " + shipmentId
                ));
    }

    private User findLogisticsOperator(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> notFound("Driver not found with id: " + userId));

        if (!"LOGISTICS_OPERATOR".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected driver must have the LOGISTICS_OPERATOR role"
            );
        }

        return user;
    }

    /**
     * SecurityConfig already protects /api/routes/**. This second guard keeps
     * service calls safe outside the HTTP filter chain as well.
     */
    private Authentication requireRouteManager() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        boolean isRouteManager = authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_LOGISTICS_OPERATOR")
                        || authority.equals("ROLE_ADMINISTRATOR"));

        if (!isRouteManager) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only logistics operators and administrators can manage routes"
            );
        }

        return authentication;
    }

    /**
     * Routes are normally created by the operator handling the shipment. For
     * legacy or newly created shipments that have not yet been assigned, the
     * first logistics operator to create the route becomes the assignee.
     */
    private void assignCurrentOperatorWhenUnassigned(
            Shipment shipment,
            Authentication authentication
    ) {
        if (isAdministrator(authentication) || shipment.getAssignedOperator() != null) {
            return;
        }

        shipment.setAssignedOperator(currentOperator(authentication));
    }

    private void requireAssignedOperatorOrAdministrator(
            Shipment shipment,
            Authentication authentication
    ) {
        if (isAdministrator(authentication)) {
            return;
        }

        User assignedOperator = shipment.getAssignedOperator();
        User currentOperator = currentOperator(authentication);

        if (assignedOperator == null
                || !assignedOperator.getId().equals(currentOperator.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This route belongs to another logistics operator"
            );
        }
    }

    private User currentOperator(Authentication authentication) {
        User user = findCurrentUser(authentication);

        if (!"LOGISTICS_OPERATOR".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The authenticated user is not a logistics operator"
            );
        }

        return user;
    }

    private User findCurrentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Authenticated user account was not found"
                ));
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMINISTRATOR"::equals);
    }

    private RouteResponse toResponse(Route route) {
        Shipment shipment = route.getShipment();
        User driver = route.getDriver();

        return RouteResponse.builder()
                .id(route.getId())
                .shipmentId(shipment.getId())
                .trackingNumber(shipment.getTrackingNumber())
                .driverId(driver == null ? null : driver.getId())
                .driverName(driver == null ? null : driver.getFullName())
                .driverEmail(driver == null ? null : driver.getEmail())
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .originLatitude(route.getOriginLatitude())
                .originLongitude(route.getOriginLongitude())
                .destinationLatitude(route.getDestinationLatitude())
                .destinationLongitude(route.getDestinationLongitude())
                .waypoints(route.getWaypoints())
                .distanceKm(route.getDistanceKm())
                .estimatedTimeMinutes(route.getEstimatedTimeMinutes())
                .actualTimeMinutes(route.getActualTimeMinutes())
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

    private String valueOrFallback(String preferred, String fallback) {
        String normalizedPreferred = blankToNull(preferred);
        return normalizedPreferred == null ? blankToNull(fallback) : normalizedPreferred;
    }

    private BigDecimal decimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException notFound(String reason) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
    }
}
