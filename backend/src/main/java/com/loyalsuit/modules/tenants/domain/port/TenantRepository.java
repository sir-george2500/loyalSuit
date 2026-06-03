package com.loyalsuit.modules.tenants.domain.port;

import com.loyalsuit.modules.tenants.domain.Tenant;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(UUID id);
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
