package com.loyalsuit.modules.promotions.infrastructure.persistence;

import com.loyalsuit.modules.promotions.domain.CouponRedemption;
import com.loyalsuit.modules.promotions.domain.port.CouponRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponRedemptionRepositoryAdapter implements CouponRedemptionRepository {

    private final CouponRedemptionJpaRepository jpa;

    @Override
    public CouponRedemption save(CouponRedemption redemption) {
        return jpa.save(redemption);
    }

    @Override
    public long countByCouponId(UUID couponId) {
        return jpa.countByCouponId(couponId);
    }

    @Override
    public long countByCouponIdAndCustomerEmail(UUID couponId, String customerEmail) {
        return jpa.countByCouponIdAndCustomerEmail(couponId, customerEmail);
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpa.existsByOrderId(orderId);
    }
}
