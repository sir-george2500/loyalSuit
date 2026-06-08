package com.loyalsuit.modules.pos.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Closes a drawer with the cash counted out of it; the service reconciles the variance. */
@Getter
@Setter
public class CloseShiftRequest {

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal countedCash;
}
