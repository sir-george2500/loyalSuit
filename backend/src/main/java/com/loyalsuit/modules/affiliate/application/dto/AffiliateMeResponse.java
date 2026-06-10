package com.loyalsuit.modules.affiliate.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** An affiliate's own view: their referral code, reward rate, and earnings to date. */
public record AffiliateMeResponse(
        UUID id,
        String code,
        BigDecimal rewardRate,
        boolean active,
        BigDecimal earned,
        BigDecimal reversed) {
}
