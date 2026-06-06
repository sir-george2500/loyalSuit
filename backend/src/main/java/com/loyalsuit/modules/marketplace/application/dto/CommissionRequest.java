package com.loyalsuit.modules.marketplace.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommissionRequest {

    @NotNull(message = "Commission rate is required")
    @DecimalMin(value = "0.0", message = "Commission cannot be negative")
    @DecimalMax(value = "100.0", message = "Commission cannot exceed 100%")
    private BigDecimal rate;
}
