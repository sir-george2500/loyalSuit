package com.loyalsuit.modules.promotions.domain;

/** How a coupon discounts an order. */
public enum DiscountType {
    /** A percentage off the subtotal (value is 0–100), optionally capped by maxDiscount. */
    PERCENT,
    /** A flat amount off the subtotal (value is the amount). */
    FIXED_AMOUNT
}
