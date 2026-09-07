package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.RouteAlternativeDTO;
import com.shiptrack.shiptrackpro.integration.maps.GoogleMapsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** Selects the route that should be used at the present departure time. */
@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final GoogleMapsClient googleMapsClient;

    public RouteOptimizationResult optimize(String origin, String destination) {
        List<RouteAlternativeDTO> alternatives = googleMapsClient
                .calculateAlternativeRoutes(origin, destination);

        if (alternatives.isEmpty()) {
            return new RouteOptimizationResult(null,
                    "No live route alternatives were available; the route was saved using the shipment addresses.",
                    0);
        }

        RouteAlternativeDTO selected = alternatives.stream()
                .min(Comparator.comparing(
                        alternative -> alternative.trafficAdjustedDurationMinutes() == null
                                ? Integer.MAX_VALUE
                                : alternative.trafficAdjustedDurationMinutes()))
                .orElseThrow();

        String reason = alternatives.size() == 1
                ? "Selected the available route using the current traffic estimate."
                : "Selected the route with the lowest traffic-adjusted duration from "
                + alternatives.size() + " alternatives.";

        return new RouteOptimizationResult(selected, reason, alternatives.size());
    }

    public record RouteOptimizationResult(
            RouteAlternativeDTO selectedAlternative,
            String selectionReason,
            int alternativeCount
    ) {
    }
}
