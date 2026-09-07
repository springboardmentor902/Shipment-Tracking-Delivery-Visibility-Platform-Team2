package com.shiptrack.shiptrackpro.integration.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiptrack.shiptrackpro.dto.RouteAlternativeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * Small HTTP-only Google Maps implementation. The API key is resolved from
 * GOOGLE_MAPS_API_KEY (or the equivalent Spring property) and is never logged.
 */
@Service
public class GoogleMapsHttpClient implements GoogleMapsClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsHttpClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String apiKey;
    private final String geocodingUrl;
    private final String directionsUrl;

    public GoogleMapsHttpClient(
            @Value("${google.maps.api-key:${GOOGLE_MAPS_API_KEY:}}") String apiKey,
            @Value("${google.maps.geocoding-url:https://maps.googleapis.com/maps/api/geocode/json}")
            String geocodingUrl,
            @Value("${google.maps.directions-url:https://maps.googleapis.com/maps/api/directions/json}")
            String directionsUrl
    ) {
        this.apiKey = apiKey;
        this.geocodingUrl = geocodingUrl;
        this.directionsUrl = directionsUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Optional<GeoCoordinates> geocode(String address) {
        if (!isConfigured() || address == null || address.isBlank()) {
            return Optional.empty();
        }

        URI uri = URI.create(
                geocodingUrl
                        + "?address=" + encode(address)
                        + "&key=" + encode(apiKey)
        );

        return executeGoogleRequest(uri)
                .flatMap(this::toCoordinates);
    }

    @Override
    public Optional<RouteMetrics> getDirections(
            GeoCoordinates origin,
            GeoCoordinates destination
    ) {
        return getAlternativeRoutes(origin, destination)
                .stream()
                .findFirst()
                .map(route -> new RouteMetrics(
                        route.distanceKm(),
                        route.trafficAdjustedDurationMinutes() == null
                                ? route.durationMinutes()
                                : route.trafficAdjustedDurationMinutes()));
    }

    @Override
    public List<RouteAlternativeDTO> getAlternativeRoutes(
            GeoCoordinates origin,
            GeoCoordinates destination
    ) {
        if (!isConfigured() || origin == null || destination == null) {
            return List.of();
        }

        String originCoordinates = origin.latitude() + "," + origin.longitude();
        String destinationCoordinates = destination.latitude() + "," + destination.longitude();

        URI uri = URI.create(
                directionsUrl
                        + "?origin=" + encode(originCoordinates)
                        + "&destination=" + encode(destinationCoordinates)
                        + "&departure_time=now"
                        + "&alternatives=true"
                        + "&units=metric"
                        + "&key=" + encode(apiKey)
        );

        return executeGoogleRequest(uri)
                .map(this::toRouteAlternatives)
                .orElseGet(List::of);
    }

    private boolean isConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Google Maps is not configured; route distance and ETA will remain empty");
            return false;
        }
        return true;
    }

    private Optional<JsonNode> executeGoogleRequest(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Google Maps request returned HTTP {}", response.statusCode());
                return Optional.empty();
            }

            JsonNode body = objectMapper.readTree(response.body());
            if (!"OK".equals(body.path("status").asText())) {
                log.warn("Google Maps request did not return a route result (status={})",
                        body.path("status").asText("unknown"));
                return Optional.empty();
            }

            return Optional.of(body);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Google Maps request was interrupted");
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            log.warn("Google Maps request failed: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeoCoordinates> toCoordinates(JsonNode root) {
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            return Optional.empty();
        }

        JsonNode location = results.get(0)
                .path("geometry")
                .path("location");

        if (!location.hasNonNull("lat") || !location.hasNonNull("lng")) {
            return Optional.empty();
        }

        return Optional.of(new GeoCoordinates(
                location.path("lat").asDouble(),
                location.path("lng").asDouble()
        ));
    }

    private Optional<RouteMetrics> toRouteMetrics(JsonNode root) {
        return toRouteAlternatives(root).stream()
                .findFirst()
                .map(route -> new RouteMetrics(route.distanceKm(),
                        route.trafficAdjustedDurationMinutes() == null
                                ? route.durationMinutes()
                                : route.trafficAdjustedDurationMinutes()));
    }

    private List<RouteAlternativeDTO> toRouteAlternatives(JsonNode root) {
        List<RouteAlternativeDTO> alternatives = new ArrayList<>();
        JsonNode routes = root.path("routes");
        if (!routes.isArray()) {
            return alternatives;
        }

        for (JsonNode route : routes) {
            JsonNode legs = route.path("legs");
            if (!legs.isArray() || legs.isEmpty()) {
                continue;
            }

            long distanceMeters = 0;
            long standardSeconds = 0;
            long trafficSeconds = 0;
            boolean hasTrafficDuration = true;

            for (JsonNode leg : legs) {
                JsonNode distance = leg.path("distance").path("value");
                JsonNode duration = leg.path("duration").path("value");
                JsonNode trafficDuration = leg.path("duration_in_traffic").path("value");
                if (!distance.canConvertToLong() || !duration.canConvertToLong()) {
                    distanceMeters = -1;
                    break;
                }
                distanceMeters += distance.asLong();
                standardSeconds += duration.asLong();
                if (trafficDuration.canConvertToLong()) {
                    trafficSeconds += trafficDuration.asLong();
                } else {
                    hasTrafficDuration = false;
                }
            }

            if (distanceMeters < 0 || standardSeconds <= 0) {
                continue;
            }

            BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters)
                    .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
            Integer durationMinutes = toMinutes(standardSeconds);
            Integer trafficMinutes = hasTrafficDuration ? toMinutes(trafficSeconds) : durationMinutes;
            String summary = route.path("summary").asText("Suggested route");

            alternatives.add(new RouteAlternativeDTO(
                    distanceKm, durationMinutes, trafficMinutes, summary));
        }

        return alternatives;
    }

    private int toMinutes(long seconds) {
        return Math.max(1, (int) Math.ceil(seconds / 60.0));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
