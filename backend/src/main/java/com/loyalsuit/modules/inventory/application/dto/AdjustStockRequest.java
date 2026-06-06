package com.loyalsuit.modules.inventory.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Applies a signed change to a stock row's quantity (e.g. +50 received, -3 damaged). */
@Data
public class AdjustStockRequest {

    @NotNull(message = "Delta is required")
    private Integer delta;
}
