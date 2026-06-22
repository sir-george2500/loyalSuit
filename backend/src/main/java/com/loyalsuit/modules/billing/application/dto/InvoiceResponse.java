package com.loyalsuit.modules.billing.application.dto;

import com.loyalsuit.modules.orders.domain.PaymentMethod;
import com.loyalsuit.modules.orders.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A full invoice rendered from an order and its line items. */
public record InvoiceResponse(
        UUID orderId,
        String invoiceNumber,
        String orderNumber,
        Instant issuedAt,
        String customerName,
        String customerEmail,
        String customerPhone,
        PaymentMethod method,
        PaymentStatus status,
        String currency,
        List<InvoiceLine> items,
        BigDecimal subtotal,
        BigDecimal shippingAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal total) {}
