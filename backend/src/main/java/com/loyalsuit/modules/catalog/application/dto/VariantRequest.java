package com.loyalsuit.modules.catalog.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Writable fields for a product variant. Stock is intentionally excluded — it is
 * owned by the inventory module (per-warehouse), not set on the variant here.
 */
@Data
public class VariantRequest {

    @NotBlank(message = "Variant name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 100)
    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be positive")
    private BigDecimal price;
}
