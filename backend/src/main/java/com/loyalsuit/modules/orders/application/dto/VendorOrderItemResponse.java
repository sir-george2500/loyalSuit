package com.loyalsuit.modules.orders.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** One of a vendor's lines on an order. Product name is resolved for display. */
public record VendorOrderItemResponse(
        UUID productId,
        String productName,
        UUID variantId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total) {
}
