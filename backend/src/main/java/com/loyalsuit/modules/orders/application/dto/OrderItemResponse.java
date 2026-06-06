package com.loyalsuit.modules.orders.application.dto;

import com.loyalsuit.modules.orders.domain.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        UUID variantId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(), item.getVariantId(), item.getQuantity(),
                item.getUnitPrice(), item.getTotal());
    }
}
