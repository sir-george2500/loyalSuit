package com.loyalsuit.modules.marketplace.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A vendor's commission summary. {@code earnedBalance} is what the vendor is
 * currently owed (sum of EARNED net amounts) — the ceiling a payout draws against
 * in slice 5d. {@code reversedTotal} is the net clawed back by refunds.
 */
public record VendorEarningsResponse(
        UUID vendorId,
        BigDecimal earnedBalance,
        BigDecimal reversedTotal) {
}
