package com.loyalsuit.modules.tenants.domain.port;

import com.loyalsuit.modules.tenants.domain.Tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(UUID id);
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    /** All browsable storefronts (active, ordered by name) for the public marketplace index. */
    List<Tenant> findAllActive();
}
