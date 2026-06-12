package com.loyalsuit.modules.tenants.infrastructure.persistence;

import com.loyalsuit.modules.tenants.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TenantJpaRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Tenant> findByActiveTrueOrderByNameAsc();
}
