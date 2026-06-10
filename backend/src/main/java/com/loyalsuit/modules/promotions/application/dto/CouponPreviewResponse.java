package com.loyalsuit.modules.promotions.application.dto;

import java.math.BigDecimal;

/** What a coupon would take off the current cart — a storefront preview, not a redemption. */
public record CouponPreviewResponse(
        String code,
        String description,
        BigDecimal discountAmount) {
}
