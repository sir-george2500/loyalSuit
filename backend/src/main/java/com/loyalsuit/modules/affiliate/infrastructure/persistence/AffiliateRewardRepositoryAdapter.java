package com.loyalsuit.modules.affiliate.infrastructure.persistence;

import com.loyalsuit.modules.affiliate.domain.AffiliateReward;
import com.loyalsuit.modules.affiliate.domain.RewardStatus;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRewardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AffiliateRewardRepositoryAdapter implements AffiliateRewardRepository {

    private final AffiliateRewardJpaRepository jpa;

    @Override
    public AffiliateReward save(AffiliateReward reward) {
        return jpa.save(reward);
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpa.existsByOrderId(orderId);
    }

    @Override
    public List<AffiliateReward> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId);
    }

    @Override
    public Page<AffiliateReward> findByTenantIdAndAffiliateId(UUID tenantId, UUID affiliateId, Pageable pageable) {
        return jpa.findByTenantIdAndAffiliateIdOrderByCreatedAtDesc(tenantId, affiliateId, pageable);
    }

    @Override
    public BigDecimal sumRewardAmount(UUID tenantId, UUID affiliateId, RewardStatus status) {
        return jpa.sumRewardAmount(tenantId, affiliateId, status);
    }
}
