package com.loyalsuit.modules.marketplace.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A vendor's payout standing. {@code available} = earned commission net, less what is
 * already locked in {@code pending} requests and {@code paid} out — it's the ceiling a
 * new payout request may draw against.
 */
public record PayoutBalanceResponse(
        UUID vendorId,
        BigDecimal earned,
        BigDecimal pending,
        BigDecimal paid,
        BigDecimal available) {
}
