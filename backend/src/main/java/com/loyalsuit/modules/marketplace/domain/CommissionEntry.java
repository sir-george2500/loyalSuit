package com.loyalsuit.modules.marketplace.domain;

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
 * One auditable line in the commission ledger: the platform's cut of a single
 * vendor order line. Amounts are snapshotted at settlement so the math is
 * reproducible even after the vendor's rate later changes. {@code vendorId} is the
 * vendor's user id (matching {@code products.vendor_id} / {@code order_items.vendor_id}).
 */
@Entity
@Table(name = "commission_entries")
@Getter
@Setter
@NoArgsConstructor
public class CommissionEntry extends TenantScopedEntity {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Column(name = "vendor_id", nullable = false, updatable = false)
    private UUID vendorId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "order_item_id", nullable = false, updatable = false)
    private UUID orderItemId;

    @Column(name = "order_number", nullable = false, updatable = false)
    private String orderNumber;

    @Column(name = "gross_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_rate", nullable = false, updatable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommissionStatus status = CommissionStatus.EARNED;

    /**
     * Earns a commission on a single order line: platform cut = gross × rate%,
     * rounded to cents (HALF_UP); the vendor's net is the remainder.
     */
    public CommissionEntry(UUID tenantId, UUID vendorId, UUID orderId, UUID orderItemId,
                           String orderNumber, BigDecimal grossAmount, BigDecimal commissionRate) {
        this.setTenantId(tenantId);
        this.vendorId = vendorId;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.orderNumber = orderNumber;
        this.grossAmount = grossAmount;
        this.commissionRate = commissionRate;
        this.commissionAmount = grossAmount.multiply(commissionRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        this.netAmount = grossAmount.subtract(this.commissionAmount);
        this.status = CommissionStatus.EARNED;
    }

    /** Refund: the entry stops counting toward the vendor's balance but is kept for audit. */
    public void reverse() {
        this.status = CommissionStatus.REVERSED;
    }
}
