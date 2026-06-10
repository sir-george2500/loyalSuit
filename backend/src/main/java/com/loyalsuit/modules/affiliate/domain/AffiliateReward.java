package com.loyalsuit.modules.affiliate.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * One auditable line in the affiliate reward ledger: what an affiliate earned on a referred
 * order. The rate and amounts are snapshotted at earning time so the math is reproducible
 * even after the affiliate's rate later changes. Mirrors the commission ledger.
 */
@Entity
@Table(name = "affiliate_rewards")
@Getter
@Setter
@NoArgsConstructor
public class AffiliateReward extends TenantScopedEntity {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Column(name = "affiliate_id", nullable = false, updatable = false)
    private UUID affiliateId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, updatable = false)
    private String orderNumber;

    @Column(name = "gross_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "reward_rate", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal rewardRate;

    @Column(name = "reward_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal rewardAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardStatus status = RewardStatus.EARNED;

    public AffiliateReward(UUID tenantId, UUID affiliateId, UUID orderId, String orderNumber,
                           BigDecimal grossAmount, BigDecimal rewardRate) {
        this.setTenantId(tenantId);
        this.affiliateId = affiliateId;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.grossAmount = grossAmount;
        this.rewardRate = rewardRate;
        this.rewardAmount = grossAmount.multiply(rewardRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        this.status = RewardStatus.EARNED;
    }

    /** Refund: the entry stops counting toward the affiliate's balance but is kept for audit. */
    public void reverse() {
        this.status = RewardStatus.REVERSED;
    }
}
