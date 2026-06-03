package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Category> findAllByTenantId(UUID tenantId);
    List<Category> findByParentIdAndTenantId(UUID parentId, UUID tenantId);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
}
