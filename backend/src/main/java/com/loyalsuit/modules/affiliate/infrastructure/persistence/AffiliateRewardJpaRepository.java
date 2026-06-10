package com.loyalsuit.modules.affiliate.infrastructure.persistence;

import com.loyalsuit.modules.affiliate.domain.AffiliateReward;
import com.loyalsuit.modules.affiliate.domain.RewardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

interface AffiliateRewardJpaRepository extends JpaRepository<AffiliateReward, UUID> {
    boolean existsByOrderId(UUID orderId);
    List<AffiliateReward> findByOrderId(UUID orderId);
    Page<AffiliateReward> findByTenantIdAndAffiliateIdOrderByCreatedAtDesc(
            UUID tenantId, UUID affiliateId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.rewardAmount), 0) FROM AffiliateReward r "
            + "WHERE r.tenantId = :tenantId AND r.affiliateId = :affiliateId AND r.status = :status")
    BigDecimal sumRewardAmount(@Param("tenantId") UUID tenantId, @Param("affiliateId") UUID affiliateId,
                               @Param("status") RewardStatus status);
}
