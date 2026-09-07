package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RouteAnalyticsResponse {

    private long totalRoutes;
    private double averageRouteDistanceKm;
    private double timeEstimateAccuracyPercent;
    private long routesWithActualTime;
    private RouteResponse bestPerformingRoute;
    private RouteResponse worstPerformingRoute;
}
