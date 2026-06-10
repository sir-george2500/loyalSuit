package com.loyalsuit.modules.marketplace.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A vendor's payout standing. {@code available} = earned commission net, less what is
 * already locked in {@code pending} requests and {@code paid} out — it's the ceiling a
 * new payout request may draw against. When a refund reverses commission on an
 * already-paid order, {@code available} can go negative; {@code owed} is that shortfall
 * as a positive number — a debt the vendor's future earnings clear before they can
 * withdraw again.
 */
public record PayoutBalanceResponse(
        UUID vendorId,
        BigDecimal earned,
        BigDecimal pending,
        BigDecimal paid,
        BigDecimal available,
        BigDecimal owed) {

    public static PayoutBalanceResponse of(UUID vendorId, BigDecimal earned, BigDecimal pending,
                                           BigDecimal paid, BigDecimal available) {
        BigDecimal owed = available.signum() < 0 ? available.negate() : BigDecimal.ZERO;
        return new PayoutBalanceResponse(vendorId, earned, pending, paid, available, owed);
    }
}
