package com.loyalsuit.modules.orders.application.dto;

import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String status,
        String paymentMethod,
        String paymentStatus,
        String customerName,
        String customerEmail,
        String customerPhone,
        Map<String, Object> shippingAddress,
        String notes,
        BigDecimal subtotal,
        BigDecimal total,
        String currency,
        List<OrderItemResponse> items,
        Instant createdAt) {

    public static OrderResponse from(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getPaymentMethod().name(),
                order.getPaymentStatus().name(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getShippingAddress(),
                order.getNotes(),
                order.getSubtotal(),
                order.getTotal(),
                order.getCurrency(),
                items.stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt());
    }
}
