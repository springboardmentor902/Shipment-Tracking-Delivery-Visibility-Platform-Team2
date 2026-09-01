package com.shiptrack.shiptrackpro.integration.maps;

import java.util.Optional;

/**
 * Boundary around Google Maps so route persistence stays independent from the
 * external service and the integration can be replaced or mocked in tests.
 */
public interface GoogleMapsClient {

    Optional<GeoCoordinates> geocode(String address);

    Optional<RouteMetrics> getDirections(
            GeoCoordinates origin,
            GeoCoordinates destination
    );

    default Optional<RouteMetrics> calculateRoute(
            String originAddress,
            String destinationAddress
    ) {
        return geocode(originAddress)
                .flatMap(origin -> geocode(destinationAddress)
                        .flatMap(destination -> getDirections(origin, destination)));
    }
}
