package com.loyalsuit.modules.billing.application.dto;

import com.loyalsuit.modules.orders.domain.PaymentMethod;
import com.loyalsuit.modules.orders.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A payment record, derived from an order's payment fields. */
public record PaymentResponse(
        UUID orderId,
        String orderNumber,
        String customerName,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        Instant date) {}
