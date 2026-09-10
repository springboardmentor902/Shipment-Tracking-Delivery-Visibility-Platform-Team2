package com.shiptrack.shiptrackpro.dto;

import java.math.BigDecimal;

/** A route option returned by the configured map provider. */
public record RouteAlternativeDTO(
        BigDecimal distanceKm,
        Integer durationMinutes,
        Integer trafficAdjustedDurationMinutes,
        String routeSummary
) {
}
