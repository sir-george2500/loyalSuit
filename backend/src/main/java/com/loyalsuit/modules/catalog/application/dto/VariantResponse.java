package com.loyalsuit.modules.catalog.application.dto;

import com.loyalsuit.modules.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        UUID productId,
        String name,
        String sku,
        BigDecimal price,
        Instant createdAt) {

    public static VariantResponse from(ProductVariant variant) {
        return new VariantResponse(
                variant.getId(),
                variant.getProductId(),
                variant.getName(),
                variant.getSku(),
                variant.getPrice(),
                variant.getCreatedAt());
    }
}
