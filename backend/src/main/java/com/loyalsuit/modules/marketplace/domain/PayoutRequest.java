package com.loyalsuit.modules.marketplace.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A vendor's request to be paid out a slice of their settled commission. {@code vendorId}
 * is the vendor's user id (matching the commission ledger). The decision fields
 * ({@code decidedBy} / {@code decidedAt} / {@code resolutionNote} / {@code reference})
 * plus the audit log form the trail. {@code version} guards the pay/reject decision
 * against a concurrent double-decision (the second writer fails with a 409).
 */
@Entity
@Table(name = "payout_requests")
@Getter
@Setter
@NoArgsConstructor
public class PayoutRequest extends TenantScopedEntity {

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status = PayoutStatus.PENDING;

    /** Free-text reference for the cash transfer, recorded when paid. */
    @Column
    private String reference;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public PayoutRequest(UUID tenantId, UUID vendorId, BigDecimal amount) {
        this.setTenantId(tenantId);
        this.vendorId = vendorId;
        this.amount = amount;
        this.status = PayoutStatus.PENDING;
    }
}
