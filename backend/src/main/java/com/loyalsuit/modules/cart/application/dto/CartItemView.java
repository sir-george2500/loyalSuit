package com.loyalsuit.modules.cart.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemView(
        UUID productId,
        UUID variantId,
        String productName,
        String productSlug,
        String variantName,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
