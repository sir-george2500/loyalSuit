package com.loyalsuit.modules.catalog.domain.port;

import com.loyalsuit.modules.catalog.domain.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Category> findBySlugAndTenantId(String slug, UUID tenantId);
    List<Category> findAllByTenantId(UUID tenantId);
    List<Category> findByParentIdAndTenantId(UUID parentId, UUID tenantId);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
    void deleteById(UUID id);
}
