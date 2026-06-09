package com.loyalsuit.modules.pos.application.dto;

import com.loyalsuit.modules.pos.domain.PosSale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A completed POS sale (receipt header). {@code orderNumber} is the receipt number and
 * the key into the shared order ledger; line items live on that order.
 */
public record PosSaleResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID cashierId,
        String currency,
        BigDecimal subtotal,
        BigDecimal total,
        BigDecimal amountTendered,
        BigDecimal changeGiven,
        BigDecimal cardAmount,
        int itemCount,
        Instant createdAt) {

    public static PosSaleResponse from(PosSale sale, String currency) {
        return new PosSaleResponse(
                sale.getId(),
                sale.getOrderId(),
                sale.getOrderNumber(),
                sale.getCashierId(),
                currency,
                sale.getSubtotal(),
                sale.getTotal(),
                sale.getAmountTendered(),
                sale.getChangeGiven(),
                sale.getCardAmount(),
                sale.getItemCount(),
                sale.getCreatedAt());
    }
}
