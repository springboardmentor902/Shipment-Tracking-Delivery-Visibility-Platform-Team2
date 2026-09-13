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
 * Small HTTP map client. It tries Google Maps first and uses Geoapify when
 * Google is unavailable. API keys are read from the environment and never logged.
 */
@Service
public class GoogleMapsHttpClient implements GoogleMapsClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsHttpClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String apiKey;
    private final String geocodingUrl;
    private final String directionsUrl;
    private final String geoapifyGeocodingKey;
    private final String geoapifyRoutingKey;
    private final String geoapifyGeocodingUrl;
    private final String geoapifyRoutingUrl;

    public GoogleMapsHttpClient(
            @Value("${google.maps.api-key:${GOOGLE_MAPS_API_KEY:}}") String apiKey,
            @Value("${google.maps.geocoding-url:https://maps.googleapis.com/maps/api/geocode/json}")
            String geocodingUrl,
            @Value("${google.maps.directions-url:https://maps.googleapis.com/maps/api/directions/json}")
            String directionsUrl,
            @Value("${geoapify.geocoding-api-key:}") String geoapifyGeocodingKey,
            @Value("${geoapify.routing-api-key:}") String geoapifyRoutingKey,
            @Value("${geoapify.geocoding-url:https://api.geoapify.com/v1/geocode/search}")
            String geoapifyGeocodingUrl,
            @Value("${geoapify.routing-url:https://api.geoapify.com/v1/routing}")
            String geoapifyRoutingUrl
    ) {
        this.apiKey = apiKey;
        this.geocodingUrl = geocodingUrl;
        this.directionsUrl = directionsUrl;
        this.geoapifyGeocodingKey = geoapifyGeocodingKey;
        this.geoapifyRoutingKey = geoapifyRoutingKey;
        this.geoapifyGeocodingUrl = geoapifyGeocodingUrl;
        this.geoapifyRoutingUrl = geoapifyRoutingUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public Optional<GeoCoordinates> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        if (hasText(apiKey)) {
            URI uri = URI.create(
                    geocodingUrl
                            + "?address=" + encode(address)
                            + "&key=" + encode(apiKey)
            );

            Optional<GeoCoordinates> googleResult = executeGoogleRequest(uri)
                    .flatMap(this::toCoordinates);
            if (googleResult.isPresent()) {
                return googleResult;
            }
        }

        return geocodeWithGeoapify(address);
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
        if (origin == null || destination == null) {
            return List.of();
        }

        if (hasText(apiKey)) {
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

            List<RouteAlternativeDTO> googleRoutes = executeGoogleRequest(uri)
                    .map(this::toRouteAlternatives)
                    .orElseGet(List::of);
            if (!googleRoutes.isEmpty()) {
                return googleRoutes;
            }
        }

        return getGeoapifyRoute(origin, destination);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Optional<JsonNode> executeGoogleRequest(URI uri) {
        return executeRequest(uri, "Google Maps")
                .filter(body -> {
                    boolean successful = "OK".equals(body.path("status").asText());
                    if (!successful) {
                        log.warn("Google Maps did not return a route result (status={})",
                                body.path("status").asText("unknown"));
                    }
                    return successful;
                });
    }

    private Optional<JsonNode> executeRequest(URI uri, String provider) {
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
                log.warn("{} request returned HTTP {}", provider, response.statusCode());
                return Optional.empty();
            }

            return Optional.of(objectMapper.readTree(response.body()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("{} request was interrupted", provider);
            return Optional.empty();
        } catch (IOException | RuntimeException exception) {
            log.warn("{} request failed: {}", provider, exception.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeoCoordinates> geocodeWithGeoapify(String address) {
        if (!hasText(geoapifyGeocodingKey)) {
            return Optional.empty();
        }

        URI uri = URI.create(geoapifyGeocodingUrl
                + "?text=" + encode(address)
                + "&filter=countrycode:in"
                + "&format=json"
                + "&limit=1"
                + "&apiKey=" + encode(geoapifyGeocodingKey));

        return executeRequest(uri, "Geoapify")
                .flatMap(body -> {
                    JsonNode results = body.path("results");
                    if (!results.isArray() || results.isEmpty()) {
                        return Optional.empty();
                    }
                    JsonNode place = results.get(0);
                    if (!place.hasNonNull("lat") || !place.hasNonNull("lon")) {
                        return Optional.empty();
                    }
                    return Optional.of(new GeoCoordinates(
                            place.path("lat").asDouble(),
                            place.path("lon").asDouble()));
                });
    }

    private List<RouteAlternativeDTO> getGeoapifyRoute(
            GeoCoordinates origin,
            GeoCoordinates destination
    ) {
        if (!hasText(geoapifyRoutingKey)) {
            return List.of();
        }

        String waypoints = origin.latitude() + "," + origin.longitude()
                + "|" + destination.latitude() + "," + destination.longitude();
        URI uri = URI.create(geoapifyRoutingUrl
                + "?waypoints=" + encode(waypoints)
                + "&mode=drive"
                + "&apiKey=" + encode(geoapifyRoutingKey));

        return executeRequest(uri, "Geoapify")
                .map(body -> {
                    JsonNode features = body.path("features");
                    if (!features.isArray() || features.isEmpty()) {
                        return List.<RouteAlternativeDTO>of();
                    }
                    JsonNode properties = features.get(0).path("properties");
                    double distanceMeters = properties.path("distance").asDouble(0);
                    double durationSeconds = properties.path("time").asDouble(0);
                    if (distanceMeters <= 0 || durationSeconds <= 0) {
                        return List.<RouteAlternativeDTO>of();
                    }

                    BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters / 1000)
                            .setScale(2, RoundingMode.HALF_UP);
                    int minutes = Math.max(1, (int) Math.ceil(durationSeconds / 60));
                    return List.of(new RouteAlternativeDTO(
                            distanceKm, minutes, minutes, "Geoapify road route"));
                })
                .orElseGet(List::of);
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
