package com.loyalsuit.modules.affiliate.application.dto;

import com.loyalsuit.modules.affiliate.domain.AffiliateReward;

import java.math.BigDecimal;
import java.time.Instant;

/** One line of an affiliate's reward ledger. */
public record AffiliateRewardResponse(
        String orderNumber,
        BigDecimal grossAmount,
        BigDecimal rewardRate,
        BigDecimal rewardAmount,
        String status,
        Instant earnedAt) {

    public static AffiliateRewardResponse from(AffiliateReward reward) {
        return new AffiliateRewardResponse(
                reward.getOrderNumber(),
                reward.getGrossAmount(),
                reward.getRewardRate(),
                reward.getRewardAmount(),
                reward.getStatus().name(),
                reward.getCreatedAt());
    }
}
