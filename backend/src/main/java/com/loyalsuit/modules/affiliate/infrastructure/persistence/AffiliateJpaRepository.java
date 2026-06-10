package com.loyalsuit.modules.affiliate.infrastructure.persistence;

import com.loyalsuit.modules.affiliate.domain.Affiliate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AffiliateJpaRepository extends JpaRepository<Affiliate, UUID> {
    Optional<Affiliate> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Affiliate> findByUserId(UUID userId);
    Optional<Affiliate> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByUserId(UUID userId);
    Page<Affiliate> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
