package com.loyalsuit.modules.promotions.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A validated coupon ready to apply to a checkout: the discount to subtract now, and the
 * id/code needed to record the redemption once the order exists.
 */
public record AppliedCoupon(UUID couponId, String code, BigDecimal discount) {
}
