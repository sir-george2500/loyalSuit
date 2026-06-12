package com.loyalsuit.modules.storefront.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One hit in a cross-store marketplace search: the product plus the store that sells it, so the UI
 * can show which shop it belongs to and link to {@code /store/{storeSlug}/products/{productSlug}}.
 * Carries the store's currency so each card formats its own price correctly.
 */
public record MarketplaceProductResult(
        UUID productId,
        String productName,
        String productSlug,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String imageUrl,
        String storeName,
        String storeSlug,
        String currency) {
}
