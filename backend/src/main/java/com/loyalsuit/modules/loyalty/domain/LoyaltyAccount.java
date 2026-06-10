package com.loyalsuit.modules.loyalty.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A registered customer's loyalty points balance (one per customer per tenant). Points are
 * earned when their orders are paid and spent against discounts. The running balance is
 * kept here (with a {@link Version} for concurrent updates) and every movement is also
 * written to the {@link LoyaltyTransaction} ledger for audit.
 */
@Entity
@Table(name = "loyalty_accounts")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyAccount extends TenantScopedEntity {

    @Column(name = "customer_id", nullable = false, unique = true, updatable = false)
    private UUID customerId;

    @Column(nullable = false)
    private int points;

    @Version
    @Column(nullable = false)
    private long version;

    public LoyaltyAccount(UUID tenantId, UUID customerId) {
        this.setTenantId(tenantId);
        this.customerId = customerId;
    }

    public void credit(int amount) {
        this.points += amount;
    }

    /** Spend or claw back points; never drops the balance below zero. */
    public int debit(int amount) {
        int removed = Math.min(amount, points);
        this.points -= removed;
        return removed;
    }
}
