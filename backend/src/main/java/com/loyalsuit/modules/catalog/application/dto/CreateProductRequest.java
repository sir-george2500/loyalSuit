package com.loyalsuit.modules.catalog.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class CreateProductRequest {

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

    // Flash deal: a time-boxed sale price. The service validates it (below the list price,
    // end after start). Null sale price clears any scheduled deal.
    @DecimalMin(value = "0.00", inclusive = false, message = "Sale price must be positive")
    private BigDecimal salePrice;
    private Instant saleStartsAt;
    private Instant saleEndsAt;

    private UUID categoryId;
    // vendorId is intentionally NOT accepted from the client — ownership is derived
    // from the authenticated principal so a caller cannot assign products to others.
    private boolean digital = false;
}
