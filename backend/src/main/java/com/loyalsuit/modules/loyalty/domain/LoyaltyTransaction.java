package com.loyalsuit.modules.loyalty.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * One movement on a loyalty account: the signed point delta, why, and the order it relates
 * to. Append-only — the ledger reconstructs and audits the balance. EARN is keyed to an
 * order so accrual is idempotent (an order earns once); REVERSE undoes an order's EARN.
 */
@Entity
@Table(name = "loyalty_transactions")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyTransaction extends TenantScopedEntity {

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    /** The order that drove this movement; null for manual adjustments. */
    @Column(name = "order_id", updatable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private LoyaltyTransactionType type;

    /** Signed: positive for EARN, negative for REDEEM / REVERSE. */
    @Column(nullable = false, updatable = false)
    private int points;

    public LoyaltyTransaction(UUID tenantId, UUID accountId, UUID orderId,
                              LoyaltyTransactionType type, int points) {
        this.setTenantId(tenantId);
        this.accountId = accountId;
        this.orderId = orderId;
        this.type = type;
        this.points = points;
    }
}
