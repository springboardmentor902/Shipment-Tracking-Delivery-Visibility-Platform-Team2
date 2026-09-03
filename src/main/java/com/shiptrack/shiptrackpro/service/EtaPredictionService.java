package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.EtaPredictionResponse;

import java.util.Optional;

public interface EtaPredictionService {

    EtaPredictionResponse predict(Long shipmentId);

    EtaPredictionResponse getPrediction(Long shipmentId);

    /**
     * Recalculates only when a route with usable ETA data exists. This makes
     * tracking updates safe even when dispatch has not planned a route yet.
     */
    Optional<EtaPredictionResponse> recalculateAfterTrackingEvent(Long shipmentId);

    int recalculateInProgressShipments();
}
