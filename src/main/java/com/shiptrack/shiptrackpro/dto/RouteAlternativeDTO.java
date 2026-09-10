package com.shiptrack.shiptrackpro.dto;

import java.math.BigDecimal;

/** A Google Maps route option evaluated at the current departure time. */
public record RouteAlternativeDTO(
        BigDecimal distanceKm,
        Integer durationMinutes,
        Integer trafficAdjustedDurationMinutes,
        String routeSummary
) {
}
