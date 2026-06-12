package com.loyalsuit.modules.storefront.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A product tile on the LoyalSuit marketplace. {@code soldBy} is the vendor's store name, or
 * {@code null} when LoyalSuit sells it directly (a "house" product) — the UI shows "LoyalSuit" then.
 */
public record MarketplaceProductCard(
        UUID productId,
        String name,
        String slug,
        BigDecimal price,
        BigDecimal compareAtPrice,
        String imageUrl,
        String categoryName,
        String soldBy) {
}
