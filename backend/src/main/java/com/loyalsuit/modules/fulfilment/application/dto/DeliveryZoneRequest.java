package com.loyalsuit.modules.fulfilment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Create or edit a delivery zone (an area + its fee) under a pickup point. */
@Data
public class DeliveryZoneRequest {

    @NotBlank(message = "A zone name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "A fee is required")
    @DecimalMin(value = "0.00", message = "Fee can't be negative")
    private BigDecimal fee;
}
