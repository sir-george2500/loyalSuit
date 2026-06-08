package com.loyalsuit.modules.pos.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Opens a cash drawer with the starting float counted into it. */
@Getter
@Setter
public class OpenShiftRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal openingFloat;
}
