package com.loyalsuit.modules.billing.application.dto;

import com.loyalsuit.modules.orders.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One row in the invoice list. */
public record InvoiceSummaryResponse(
        UUID orderId,
        String invoiceNumber,
        String orderNumber,
        String customerName,
        PaymentStatus status,
        BigDecimal total,
        String currency,
        Instant issuedAt) {}
