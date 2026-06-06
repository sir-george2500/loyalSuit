package com.loyalsuit.modules.cart.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

/** Sets an absolute quantity for a line; 0 removes it. */
@Data
public class UpdateItemRequest {

    @NotNull(message = "Product is required")
    private UUID productId;

    private UUID variantId;

    @Min(value = 0, message = "Quantity cannot be negative")
    private int quantity;
}
