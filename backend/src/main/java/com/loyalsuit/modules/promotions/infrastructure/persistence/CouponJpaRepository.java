package com.loyalsuit.modules.promotions.infrastructure.persistence;

import com.loyalsuit.modules.promotions.domain.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CouponJpaRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Coupon> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    Page<Coupon> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
