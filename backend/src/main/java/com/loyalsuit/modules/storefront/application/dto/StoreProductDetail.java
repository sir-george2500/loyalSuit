package com.loyalsuit.modules.storefront.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full storefront product page. Exposes only customer-facing data and an
 * {@code inStock} boolean — never exact quantities, SKUs, or internal status.
 */
public record StoreProductDetail(
        UUID id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        BigDecimal compareAtPrice,
        boolean digital,
        String categoryName,
        List<String> images,
        List<StoreVariant> variants,
        boolean inStock) {
}
