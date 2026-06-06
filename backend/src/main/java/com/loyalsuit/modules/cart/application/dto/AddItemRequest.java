package com.loyalsuit.modules.cart.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddItemRequest {

    @NotNull(message = "Product is required")
    private UUID productId;

    private UUID variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
