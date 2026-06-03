package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    @Override
    public Category save(Category category) {
        return jpa.save(category);
    }

    @Override
    public Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Category> findAllByTenantId(UUID tenantId) {
        return jpa.findAllByTenantId(tenantId);
    }

    @Override
    public List<Category> findByParentIdAndTenantId(UUID parentId, UUID tenantId) {
        return jpa.findByParentIdAndTenantId(parentId, tenantId);
    }

    @Override
    public boolean existsBySlugAndTenantId(String slug, UUID tenantId) {
        return jpa.existsBySlugAndTenantId(slug, tenantId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
