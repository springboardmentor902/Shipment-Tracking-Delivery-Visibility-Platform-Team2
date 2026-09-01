package com.shiptrack.shiptrackpro.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically refreshes ETAs even when a shipment receives no new update. */
@Component
@RequiredArgsConstructor
public class EtaPredictionScheduler {

    private final EtaPredictionService etaPredictionService;

    @Value("${app.eta.scheduler-enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Defaults to 15 minutes, and can be overridden with
     * ETA_RECALCULATION_FIXED_DELAY_MS (or the corresponding Spring property).
     */
    @Scheduled(
            fixedDelayString = "${app.eta.recalculation-fixed-delay-ms:900000}",
            initialDelayString = "${app.eta.recalculation-initial-delay-ms:60000}"
    )
    public void recalculateInProgressShipments() {
        if (schedulerEnabled) {
            etaPredictionService.recalculateInProgressShipments();
        }
    }
}
