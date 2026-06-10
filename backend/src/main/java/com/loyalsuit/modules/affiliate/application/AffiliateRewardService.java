package com.loyalsuit.modules.affiliate.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateMeResponse;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateRewardResponse;
import com.loyalsuit.modules.affiliate.domain.Affiliate;
import com.loyalsuit.modules.affiliate.domain.AffiliateReward;
import com.loyalsuit.modules.affiliate.domain.RewardStatus;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRepository;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The affiliate reward engine and ledger. A reward is <b>earned</b> when a referred order is
 * paid (rate × order value, snapshotted) and <b>reversed</b> if that order is refunded —
 * mirroring the commission engine. Earning and reversal are idempotent per order. An
 * affiliate's balance is the sum of EARNED rewards.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AffiliateRewardService {

    private final AffiliateRepository affiliateRepository;
    private final AffiliateRewardRepository rewardRepository;

    /** Earn the affiliate's reward on a just-paid referred order (idempotent per order). */
    @Transactional
    public void earnForOrder(UUID tenantId, UUID affiliateId, UUID orderId, String orderNumber, BigDecimal grossAmount) {
        if (affiliateId == null || rewardRepository.existsByOrderId(orderId)) {
            return;
        }
        affiliateRepository.findByIdAndTenantId(affiliateId, tenantId).ifPresent(affiliate ->
                rewardRepository.save(new AffiliateReward(
                        tenantId, affiliateId, orderId, orderNumber, grossAmount, affiliate.getRewardRate())));
    }

    /** Reverse the reward(s) of a refunded order (idempotent; already-reversed stay reversed). */
    @Transactional
    public void reverseForOrder(UUID tenantId, UUID orderId) {
        List<AffiliateReward> earned = rewardRepository.findByOrderId(orderId).stream()
                .filter(reward -> reward.getStatus() == RewardStatus.EARNED)
                .toList();
        for (AffiliateReward reward : earned) {
            reward.reverse();
            rewardRepository.save(reward);
        }
    }

    /** The affiliate's own standing (code, rate, earnings) — 404 if the user isn't an affiliate. */
    public AffiliateMeResponse me(UUID tenantId, UUID userId) {
        Affiliate affiliate = requireAffiliate(userId);
        return new AffiliateMeResponse(
                affiliate.getId(),
                affiliate.getCode(),
                affiliate.getRewardRate(),
                affiliate.isActive(),
                rewardRepository.sumRewardAmount(tenantId, affiliate.getId(), RewardStatus.EARNED),
                rewardRepository.sumRewardAmount(tenantId, affiliate.getId(), RewardStatus.REVERSED));
    }

    public PageResponse<AffiliateRewardResponse> myRewards(UUID tenantId, UUID userId, Pageable pageable) {
        Affiliate affiliate = requireAffiliate(userId);
        return new PageResponse<>(rewardRepository
                .findByTenantIdAndAffiliateId(tenantId, affiliate.getId(), pageable)
                .map(AffiliateRewardResponse::from));
    }

    private Affiliate requireAffiliate(UUID userId) {
        return affiliateRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Affiliate", userId));
    }
}
