package com.loyalsuit.modules.promotions.domain;

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
 * One use of a coupon, recorded when an order redeems it. The ledger enforces usage limits
 * (total and per-customer, keyed by the lower-cased email) and audits the discount given.
 * Unique on {@code orderId} so a retried checkout can't redeem the same coupon twice.
 */
@Entity
@Table(name = "coupon_redemptions")
@Getter
@Setter
@NoArgsConstructor
public class CouponRedemption extends TenantScopedEntity {

    @Column(name = "coupon_id", nullable = false, updatable = false)
    private UUID couponId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    /** Lower-cased customer email at redemption, for per-customer limits; null if none given. */
    @Column(name = "customer_email", updatable = false)
    private String customerEmail;

    @Column(name = "discount_amount", nullable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    public CouponRedemption(UUID tenantId, UUID couponId, UUID orderId, String customerEmail,
                            BigDecimal discountAmount) {
        this.setTenantId(tenantId);
        this.couponId = couponId;
        this.orderId = orderId;
        this.customerEmail = customerEmail == null ? null : customerEmail.trim().toLowerCase();
        this.discountAmount = discountAmount;
    }
}
