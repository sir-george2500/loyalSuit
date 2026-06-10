package com.loyalsuit.modules.promotions.domain;

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
import java.time.Instant;
import java.util.UUID;

/**
 * A discount code. Codes are unique per tenant (stored upper-cased). A coupon may be
 * limited by a minimum subtotal, a discount cap (for percentages), a validity window, and
 * usage limits (total and per-customer). The discount math lives here so it's computed the
 * same way for the storefront preview and the authoritative checkout.
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
public class Coupon extends TenantScopedEntity {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Column(nullable = false, length = 64)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_subtotal", precision = 10, scale = 2)
    private BigDecimal minOrderSubtotal;

    /** Caps a PERCENT discount; ignored for FIXED_AMOUNT. */
    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    /** Total redemptions allowed across all customers; null = unlimited. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Redemptions allowed per customer (by email); null = unlimited. */
    @Column(name = "per_customer_limit")
    private Integer perCustomerLimit;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Coupon(UUID tenantId, String code, DiscountType discountType, BigDecimal value) {
        this.setTenantId(tenantId);
        this.code = normalize(code);
        this.discountType = discountType;
        this.value = value;
    }

    public void setCode(String code) {
        this.code = normalize(code);
    }

    /** The discount this coupon yields on a subtotal — never more than the subtotal itself. */
    public BigDecimal discountFor(BigDecimal subtotal) {
        BigDecimal discount = discountType == DiscountType.PERCENT
                ? subtotal.multiply(value).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : value;
        if (discountType == DiscountType.PERCENT && maxDiscount != null) {
            discount = discount.min(maxDiscount);
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    public boolean isWithinWindow(Instant now) {
        return (startsAt == null || !now.isBefore(startsAt))
                && (endsAt == null || !now.isAfter(endsAt));
    }

    public boolean meetsMinimum(BigDecimal subtotal) {
        return minOrderSubtotal == null || subtotal.compareTo(minOrderSubtotal) >= 0;
    }

    public static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
