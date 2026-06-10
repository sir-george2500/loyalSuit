package com.loyalsuit.modules.promotions.domain.port;

import com.loyalsuit.modules.promotions.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Coupon> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    Page<Coupon> findByTenantId(UUID tenantId, Pageable pageable);
}
