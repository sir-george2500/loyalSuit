package com.loyalsuit.modules.pos.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** One line a cashier rang up: a product (optionally a variant) and a quantity. */
@Getter
@Setter
public class SaleLineRequest {

    @NotNull
    private UUID productId;

    /** Optional variant; null for a simple (non-variant) product. */
    private UUID variantId;

    @Min(1)
    private int quantity;
}
