package com.loyalsuit.modules.promotions.domain.port;

import com.loyalsuit.modules.promotions.domain.CouponRedemption;

import java.util.UUID;

public interface CouponRedemptionRepository {
    CouponRedemption save(CouponRedemption redemption);
    long countByCouponId(UUID couponId);
    long countByCouponIdAndCustomerEmail(UUID couponId, String customerEmail);
    boolean existsByOrderId(UUID orderId);
}
