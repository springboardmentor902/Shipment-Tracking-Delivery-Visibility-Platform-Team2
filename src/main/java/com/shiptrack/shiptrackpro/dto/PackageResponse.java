package com.shiptrack.shiptrackpro.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PackageResponse {
    private Long id;
    private String description;
    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private Integer quantity;
    private BigDecimal declaredValue;
    private Boolean fragile;
}
