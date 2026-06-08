package com.loyalsuit.modules.pos.infrastructure.persistence;

import com.loyalsuit.modules.pos.domain.PosSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PosSaleJpaRepository extends JpaRepository<PosSale, UUID> {
    Optional<PosSale> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<PosSale> findByTenantIdAndClientSaleId(UUID tenantId, String clientSaleId);
    Page<PosSale> findByTenantId(UUID tenantId, Pageable pageable);
}
