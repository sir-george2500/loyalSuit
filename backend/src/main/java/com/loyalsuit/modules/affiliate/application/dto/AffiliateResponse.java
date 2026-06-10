package com.loyalsuit.modules.affiliate.application.dto;

import com.loyalsuit.modules.affiliate.domain.Affiliate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** An affiliate. {@code name}/{@code email} are resolved from the linked user (null if gone). */
public record AffiliateResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        String code,
        BigDecimal rewardRate,
        boolean active,
        Instant createdAt) {

    public static AffiliateResponse from(Affiliate affiliate, String name, String email) {
        return new AffiliateResponse(
                affiliate.getId(),
                affiliate.getUserId(),
                name,
                email,
                affiliate.getCode(),
                affiliate.getRewardRate(),
                affiliate.isActive(),
                affiliate.getCreatedAt());
    }
}
