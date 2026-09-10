package com.shiptrack.shiptrackpro.integration.maps;

import java.math.BigDecimal;

public record RouteMetrics(BigDecimal distanceKm, int estimatedTimeMinutes) {
}
