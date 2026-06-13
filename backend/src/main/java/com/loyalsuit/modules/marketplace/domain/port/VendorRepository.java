package com.loyalsuit.modules.marketplace.domain.port;

import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendorRepository {
    Vendor save(Vendor vendor);
    Optional<Vendor> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Vendor> findByUserId(UUID userId);
    boolean existsBySlug(String slug);
    Page<Vendor> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Vendor> findByTenantIdAndStatus(UUID tenantId, VendorStatus status, Pageable pageable);
    /** Batch lookup for attribution — resolve many vendors at once (e.g. a product listing's sellers). */
    List<Vendor> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);
}
