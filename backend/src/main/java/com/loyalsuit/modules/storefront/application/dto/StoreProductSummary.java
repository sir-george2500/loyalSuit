package com.loyalsuit.modules.storefront.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Product card for a storefront listing. Deliberately omits internal fields (SKU,
 * barcode, status, vendor) and exact stock.
 */
public record StoreProductSummary(
        UUID id,
        String name,
        String slug,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String imageUrl,
        String categoryName) {
}
