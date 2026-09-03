package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EtaPredictionResponse {

    private Long id;
    private Long shipmentId;
    private String trackingNumber;
    private LocalDateTime predictedDeliveryTime;
    private BigDecimal delayRiskScore;
    private BigDecimal confidenceScore;
    private String factors;
    private LocalDateTime calculatedAt;
}
