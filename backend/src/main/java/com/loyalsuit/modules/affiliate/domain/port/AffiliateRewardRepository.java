package com.loyalsuit.modules.affiliate.domain.port;

import com.loyalsuit.modules.affiliate.domain.AffiliateReward;
import com.loyalsuit.modules.affiliate.domain.RewardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AffiliateRewardRepository {
    AffiliateReward save(AffiliateReward reward);
    boolean existsByOrderId(UUID orderId);
    List<AffiliateReward> findByOrderId(UUID orderId);
    Page<AffiliateReward> findByTenantIdAndAffiliateId(UUID tenantId, UUID affiliateId, Pageable pageable);
    BigDecimal sumRewardAmount(UUID tenantId, UUID affiliateId, RewardStatus status);
}
