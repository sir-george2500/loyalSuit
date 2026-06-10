package com.loyalsuit.modules.promotions.infrastructure.persistence;

import com.loyalsuit.modules.promotions.domain.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CouponRedemptionJpaRepository extends JpaRepository<CouponRedemption, UUID> {
    long countByCouponId(UUID couponId);
    long countByCouponIdAndCustomerEmail(UUID couponId, String customerEmail);
    boolean existsByOrderId(UUID orderId);
}
