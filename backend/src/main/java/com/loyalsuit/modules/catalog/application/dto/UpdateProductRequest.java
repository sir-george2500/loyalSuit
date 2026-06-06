package com.loyalsuit.modules.catalog.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Editable product fields. Ownership ({@code vendorId}) and lifecycle
 * ({@code status}) are deliberately not editable here — status changes go through
 * the dedicated publish/unpublish/archive endpoints.
 */
@Data
public class UpdateProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 255)
    private String slug;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Price must be positive")
    private BigDecimal price;

    @Size(max = 100)
    private String sku;

    @Size(max = 100)
    private String barcode;

    private BigDecimal compareAtPrice;
    private UUID categoryId;
    private boolean digital = false;
}
