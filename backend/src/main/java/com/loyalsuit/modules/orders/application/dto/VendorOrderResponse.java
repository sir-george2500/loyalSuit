package com.loyalsuit.modules.orders.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An order as a vendor sees it: the order's context plus ONLY this vendor's lines and their
 * earnings on it ({@code vendorSubtotal}). Other vendors' lines and order-wide money (shipping,
 * tax, grand total) are deliberately not exposed to a seller.
 */
public record VendorOrderResponse(
        UUID orderId,
        String orderNumber,
        String status,
        String paymentStatus,
        String customerName,
        String currency,
        BigDecimal vendorSubtotal,
        List<VendorOrderItemResponse> items,
        Instant createdAt) {
}
