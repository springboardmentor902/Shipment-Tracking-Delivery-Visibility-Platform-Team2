package com.shiptrack.shiptrackpro.integration.maps;

import com.shiptrack.shiptrackpro.dto.RouteAlternativeDTO;

import java.util.List;
import java.util.Optional;

/**
 * Boundary around map providers so route persistence stays independent from
 * the external service and the integration can be mocked in tests.
 */
public interface GoogleMapsClient {

    Optional<GeoCoordinates> geocode(String address);

    Optional<RouteMetrics> getDirections(
            GeoCoordinates origin,
            GeoCoordinates destination
    );

    /** Returns all currently available route alternatives, traffic included. */
    default List<RouteAlternativeDTO> getAlternativeRoutes(
            GeoCoordinates origin,
            GeoCoordinates destination
    ) {
        return List.of();
    }

    default Optional<RouteMetrics> calculateRoute(
            String originAddress,
            String destinationAddress
    ) {
        return geocode(originAddress)
                .flatMap(origin -> geocode(destinationAddress)
                        .flatMap(destination -> getDirections(origin, destination)));
    }

    default List<RouteAlternativeDTO> calculateAlternativeRoutes(
            String originAddress,
            String destinationAddress
    ) {
        return geocode(originAddress)
                .flatMap(origin -> geocode(destinationAddress)
                        .map(destination -> getAlternativeRoutes(origin, destination)))
                .orElseGet(List::of);
    }
}
