package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.EtaPredictionResponse;
import com.shiptrack.shiptrackpro.entity.EtaPrediction;
import com.shiptrack.shiptrackpro.entity.Route;
import com.shiptrack.shiptrackpro.entity.Shipment;
import com.shiptrack.shiptrackpro.entity.TrackingEvent;
import com.shiptrack.shiptrackpro.repository.EtaPredictionRepository;
import com.shiptrack.shiptrackpro.repository.RouteRepository;
import com.shiptrack.shiptrackpro.repository.ShipmentRepository;
import com.shiptrack.shiptrackpro.repository.TrackingEventRepository;
import com.shiptrack.shiptrackpro.service.EtaPredictionService;
import com.shiptrack.shiptrackpro.service.NotificationService;
import com.shiptrack.shiptrackpro.service.ShipmentAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A small rules-based ETA calculator. It is intentionally simple so that an
 * intern can explain every score shown to the user.
 */
@Service
@RequiredArgsConstructor
public class EtaPredictionServiceImpl implements EtaPredictionService {

    private final EtaPredictionRepository etaPredictionRepository;
    private final ShipmentRepository shipmentRepository;
    private final RouteRepository routeRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final ShipmentAccessService shipmentAccessService;
    private final NotificationService notificationService;

    @Value("${app.eta.delay-risk-threshold:7.0}")
    private BigDecimal delayRiskThreshold;

    @Override
    @Transactional
    public EtaPredictionResponse predict(Long shipmentId) {
        Shipment shipment = findShipment(shipmentId);
        shipmentAccessService.requireCanManageShipment(shipment);
        return calculate(shipment, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Create a route with estimated time before calculating ETA"));
    }

    @Override
    @Transactional(readOnly = true)
    public EtaPredictionResponse getPrediction(Long shipmentId) {
        Shipment shipment = findShipment(shipmentId);
        shipmentAccessService.requireCanViewShipment(shipment);
        return etaPredictionRepository.findByShipment_Id(shipmentId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No ETA prediction exists for this shipment yet"));
    }

    @Override
    @Transactional
    public Optional<EtaPredictionResponse> recalculateAfterTrackingEvent(Long shipmentId) {
        return calculate(findShipment(shipmentId), true);
    }

    @Override
    @Transactional
    public int recalculateInProgressShipments() {
        List<String> inProgressStatuses = List.of("PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY");
        int updated = 0;
        for (Shipment shipment : shipmentRepository.findByStatusIn(inProgressStatuses)) {
            if (calculate(shipment, true).isPresent()) {
                updated++;
            }
        }
        return updated;
    }

    private Optional<EtaPredictionResponse> calculate(Shipment shipment, boolean notifyOnRisk) {
        Optional<Route> routeResult = routeRepository.findByShipmentId(shipment.getId());
        if (routeResult.isEmpty() || routeResult.get().getEstimatedTimeMinutes() == null) {
            return Optional.empty();
        }

        Route route = routeResult.get();
        List<TrackingEvent> events = trackingEventRepository
                .findByShipment_IdOrderByEventTimestampAsc(shipment.getId());
        Calculation calculation = buildCalculation(route, events);

        EtaPrediction prediction = etaPredictionRepository.findByShipment_Id(shipment.getId())
                .orElseGet(EtaPrediction::new);
        prediction.setShipment(shipment);
        prediction.setPredictedDeliveryTime(calculation.predictedDeliveryTime());
        prediction.setDelayRiskScore(calculation.delayRiskScore());
        prediction.setConfidenceScore(calculation.confidenceScore());
        prediction.setFactors(String.join("; ", calculation.factors()));
        prediction.setCalculatedAt(LocalDateTime.now());

        EtaPrediction savedPrediction = etaPredictionRepository.save(prediction);
        if (shipment.getCreatedBy() != null) {
            notificationService.send("ETA_UPDATE", shipment.getCreatedBy(), shipment);
        }
        if (notifyOnRisk && shipment.getCreatedBy() != null
                && calculation.delayRiskScore().compareTo(delayRiskThreshold) >= 0) {
            notificationService.send("DELAY_WARNING", shipment.getCreatedBy(), shipment);
        }
        return Optional.of(toResponse(savedPrediction));
    }

    private Calculation buildCalculation(Route route, List<TrackingEvent> events) {
        int remainingMinutes = Math.max(1, route.getEstimatedTimeMinutes());
        BigDecimal risk = BigDecimal.ONE;
        BigDecimal confidence = BigDecimal.valueOf(80);
        List<String> factors = new ArrayList<>();
        factors.add("Base route time: " + remainingMinutes + " minutes");

        String traffic = route.getTrafficCondition() == null ? "NORMAL"
                : route.getTrafficCondition().trim().toUpperCase(Locale.ROOT);
        if (traffic.contains("HEAVY")) {
            remainingMinutes = addPercent(remainingMinutes, 30);
            risk = risk.add(BigDecimal.valueOf(3));
            confidence = confidence.subtract(BigDecimal.valueOf(10));
            factors.add("Heavy traffic adds 30% travel time");
        } else if (traffic.contains("MODERATE")) {
            remainingMinutes = addPercent(remainingMinutes, 15);
            risk = risk.add(BigDecimal.valueOf(1.5));
            confidence = confidence.subtract(BigDecimal.valueOf(5));
            factors.add("Moderate traffic adds 15% travel time");
        } else {
            factors.add("Normal traffic");
        }

        if (route.getDistanceKm() != null && route.getDistanceKm().compareTo(BigDecimal.valueOf(500)) > 0) {
            risk = risk.add(BigDecimal.valueOf(0.5));
            factors.add("Long route distance adds a small delay risk");
        }

        if (events.isEmpty()) {
            risk = risk.add(BigDecimal.ONE);
            confidence = confidence.subtract(BigDecimal.valueOf(20));
            factors.add("No tracking events yet, so confidence is lower");
        } else {
            TrackingEvent latest = events.getLast();
            String latestStatus = latest.getStatus();
            remainingMinutes = remainingTimeForStatus(remainingMinutes, latestStatus);
            factors.add("Latest tracking status: " + latestStatus);

            long hoursSinceUpdate = Duration.between(latest.getEventTimestamp(), LocalDateTime.now()).toHours();
            if (hoursSinceUpdate >= 6) {
                remainingMinutes = addPercent(remainingMinutes, 25);
                risk = risk.add(BigDecimal.valueOf(2));
                confidence = confidence.subtract(BigDecimal.valueOf(20));
                factors.add("Last update is over 6 hours old");
            } else if (hoursSinceUpdate >= 3) {
                risk = risk.add(BigDecimal.ONE);
                confidence = confidence.subtract(BigDecimal.valueOf(10));
                factors.add("Last update is over 3 hours old");
            } else {
                factors.add("Recent tracking update increases confidence");
            }

            if ("FAILED_DELIVERY".equals(latestStatus)) {
                risk = BigDecimal.TEN;
                confidence = BigDecimal.valueOf(90);
                factors.add("A failed delivery attempt creates high delay risk");
            }
        }

        return new Calculation(
                LocalDateTime.now().plusMinutes(remainingMinutes),
                bounded(risk, BigDecimal.ZERO, BigDecimal.TEN),
                bounded(confidence, BigDecimal.ZERO, BigDecimal.valueOf(100)),
                factors
        );
    }

    private int remainingTimeForStatus(int routeMinutes, String status) {
        return switch (status) {
            case "OUT_FOR_DELIVERY" -> Math.max(15, (int) Math.ceil(routeMinutes * 0.30));
            case "IN_TRANSIT" -> Math.max(30, (int) Math.ceil(routeMinutes * 0.60));
            case "PICKED_UP" -> Math.max(30, (int) Math.ceil(routeMinutes * 0.80));
            default -> routeMinutes;
        };
    }

    private int addPercent(int minutes, int percentage) {
        return (int) Math.ceil(minutes * (1 + percentage / 100.0));
    }

    private BigDecimal bounded(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return value.max(minimum).min(maximum).setScale(2, RoundingMode.HALF_UP);
    }

    private Shipment findShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Shipment not found with id: " + shipmentId));
    }

    private EtaPredictionResponse toResponse(EtaPrediction prediction) {
        return EtaPredictionResponse.builder()
                .id(prediction.getId())
                .shipmentId(prediction.getShipment().getId())
                .trackingNumber(prediction.getShipment().getTrackingNumber())
                .predictedDeliveryTime(prediction.getPredictedDeliveryTime())
                .delayRiskScore(prediction.getDelayRiskScore())
                .confidenceScore(prediction.getConfidenceScore())
                .factors(prediction.getFactors())
                .calculatedAt(prediction.getCalculatedAt())
                .build();
    }

    private record Calculation(
            LocalDateTime predictedDeliveryTime,
            BigDecimal delayRiskScore,
            BigDecimal confidenceScore,
            List<String> factors
    ) {
    }
}
