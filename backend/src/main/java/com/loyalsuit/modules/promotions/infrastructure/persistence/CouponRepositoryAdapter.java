package com.loyalsuit.modules.promotions.infrastructure.persistence;

import com.loyalsuit.modules.promotions.domain.Coupon;
import com.loyalsuit.modules.promotions.domain.port.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryAdapter implements CouponRepository {

    private final CouponJpaRepository jpa;

    @Override
    public Coupon save(Coupon coupon) {
        return jpa.save(coupon);
    }

    @Override
    public Optional<Coupon> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<Coupon> findByTenantIdAndCode(UUID tenantId, String code) {
        return jpa.findByTenantIdAndCode(tenantId, code);
    }

    @Override
    public boolean existsByTenantIdAndCode(UUID tenantId, String code) {
        return jpa.existsByTenantIdAndCode(tenantId, code);
    }

    @Override
    public Page<Coupon> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }
}
