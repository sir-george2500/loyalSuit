package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface VendorJpaRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Vendor> findByUserId(UUID userId);
    boolean existsBySlug(String slug);
    Page<Vendor> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Vendor> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, VendorStatus status, Pageable pageable);
}
