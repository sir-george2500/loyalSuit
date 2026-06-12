package com.loyalsuit.modules.tenants.infrastructure.persistence;

import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository jpa;

    @Override
    public Tenant save(Tenant tenant) {
        return jpa.save(tenant);
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return jpa.findBySlug(slug);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpa.existsBySlug(slug);
    }

    @Override
    public List<Tenant> findAllActive() {
        return jpa.findByActiveTrueOrderByNameAsc();
    }
}
