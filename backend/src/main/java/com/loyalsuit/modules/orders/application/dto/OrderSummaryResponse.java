package com.loyalsuit.modules.orders.application.dto;

import com.loyalsuit.modules.orders.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A row in the admin order list. */
public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        String status,
        String paymentStatus,
        String customerName,
        BigDecimal total,
        String currency,
        Instant createdAt) {

    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getCustomerName(),
                order.getTotal(),
                order.getCurrency(),
                order.getCreatedAt());
    }
}
