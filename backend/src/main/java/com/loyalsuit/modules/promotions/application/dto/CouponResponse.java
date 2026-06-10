package com.loyalsuit.modules.promotions.application.dto;

import com.loyalsuit.modules.promotions.domain.Coupon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String description,
        String discountType,
        BigDecimal value,
        BigDecimal minOrderSubtotal,
        BigDecimal maxDiscount,
        Integer usageLimit,
        Integer perCustomerLimit,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        long timesRedeemed,
        Instant createdAt) {

    public static CouponResponse from(Coupon coupon, long timesRedeemed) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountType().name(),
                coupon.getValue(),
                coupon.getMinOrderSubtotal(),
                coupon.getMaxDiscount(),
                coupon.getUsageLimit(),
                coupon.getPerCustomerLimit(),
                coupon.getStartsAt(),
                coupon.getEndsAt(),
                coupon.isActive(),
                timesRedeemed,
                coupon.getCreatedAt());
    }
}
