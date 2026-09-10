package com.shiptrack.shiptrackpro.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageRequest {

    @NotBlank(message = "Package description is required")
    private String description;

    private BigDecimal weight;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    @Min(value = 1, message = "Package quantity must be at least 1")
    private Integer quantity = 1;

    private BigDecimal declaredValue;
    private Boolean fragile = false;
}
