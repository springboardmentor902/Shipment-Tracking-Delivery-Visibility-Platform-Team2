package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;

@Getter
@Builder
public class RouteAnalyticsResponse implements Serializable {

    private long totalRoutes;
    private double averageRouteDistanceKm;
    private double timeEstimateAccuracyPercent;
    private long routesWithActualTime;
    private RouteResponse bestPerformingRoute;
    private RouteResponse worstPerformingRoute;
}
