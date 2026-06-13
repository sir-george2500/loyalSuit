package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VendorJpaRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Vendor> findByUserId(UUID userId);
    Optional<Vendor> findBySlugAndTenantId(String slug, UUID tenantId);
    boolean existsBySlug(String slug);
    Page<Vendor> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Vendor> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, VendorStatus status, Pageable pageable);
    List<Vendor> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    @Query("""
            SELECT v.id FROM Vendor v
            WHERE v.tenantId = :tenantId
              AND v.status = com.loyalsuit.modules.marketplace.domain.VendorStatus.ACTIVE
            """)
    List<UUID> findActiveVendorIds(@Param("tenantId") UUID tenantId);
}
