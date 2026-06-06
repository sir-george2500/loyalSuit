package com.loyalsuit.modules.inventory.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** Sets the absolute on-hand quantity (and low-stock threshold) for a stock row. */
@Data
public class SetStockRequest {

    @NotNull(message = "Product is required")
    private UUID productId;

    /** Optional — null means product-level stock (no specific variant). */
    private UUID variantId;

    @NotNull(message = "Warehouse is required")
    private UUID warehouseId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Min(value = 0, message = "Threshold cannot be negative")
    private int lowStockThreshold = 5;
}
