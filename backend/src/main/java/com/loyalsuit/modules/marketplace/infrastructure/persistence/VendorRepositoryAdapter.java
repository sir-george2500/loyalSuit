package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VendorRepositoryAdapter implements VendorRepository {

    private final VendorJpaRepository jpa;

    @Override
    public Vendor save(Vendor vendor) {
        return jpa.save(vendor);
    }

    @Override
    public Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<Vendor> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public Optional<Vendor> findBySlugAndTenantId(String slug, UUID tenantId) {
        return jpa.findBySlugAndTenantId(slug, tenantId);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpa.existsBySlug(slug);
    }

    @Override
    public Page<Vendor> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<Vendor> findByTenantIdAndStatus(UUID tenantId, VendorStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }

    @Override
    public List<Vendor> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : jpa.findByTenantIdAndIdIn(tenantId, ids);
    }

    @Override
    public List<UUID> findActiveVendorIds(UUID tenantId) {
        return jpa.findActiveVendorIds(tenantId);
    }
}
