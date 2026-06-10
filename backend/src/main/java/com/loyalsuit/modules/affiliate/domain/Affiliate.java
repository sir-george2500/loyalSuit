package com.loyalsuit.modules.affiliate.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * An affiliate: a user who refers shoppers with a unique code and earns a reward (a
 * percentage of the referred order) when those orders are paid. One profile per user
 * per tenant; the code is unique per tenant and stored upper-cased. The affiliate's
 * name/email live on the linked user; the reward ledger lives in {@code AffiliateReward}.
 */
@Entity
@Table(name = "affiliates")
@Getter
@Setter
@NoArgsConstructor
public class Affiliate extends TenantScopedEntity {

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "reward_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rewardRate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Affiliate(UUID tenantId, UUID userId, String code, BigDecimal rewardRate) {
        this.setTenantId(tenantId);
        this.userId = userId;
        this.code = normalize(code);
        this.rewardRate = rewardRate;
    }

    public void setCode(String code) {
        this.code = normalize(code);
    }

    public static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
