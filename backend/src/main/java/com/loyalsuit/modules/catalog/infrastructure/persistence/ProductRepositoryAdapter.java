package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
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
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    @Override
    public Product save(Product product) {
        return jpa.save(product);
    }

    @Override
    public Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Product> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId) {
        return ids.isEmpty() ? List.of() : jpa.findByIdInAndTenantId(ids, tenantId);
    }

    @Override
    public Optional<Product> findBySlugAndTenantId(String slug, UUID tenantId) {
        return jpa.findBySlugAndTenantId(slug, tenantId);
    }

    @Override
    public Page<Product> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable) {
        return jpa.findByTenantIdAndVendorId(tenantId, vendorId, pageable);
    }

    @Override
    public Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    @Override
    public Page<Product> searchActive(UUID tenantId, String query, Pageable pageable) {
        return jpa.searchActive(tenantId, query, pageable);
    }

    @Override
    public Page<Product> findByTenantIdAndStatusAndCategoryId(
            UUID tenantId, ProductStatus status, UUID categoryId, Pageable pageable) {
        return jpa.findByTenantIdAndStatusAndCategoryId(tenantId, status, categoryId, pageable);
    }

    @Override
    public Page<Product> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantId(tenantId, pageable);
    }

    @Override
    public Page<Product> findByCategoryIdAndTenantId(UUID categoryId, UUID tenantId, Pageable pageable) {
        return jpa.findByCategoryIdAndTenantId(categoryId, tenantId, pageable);
    }

    @Override
    public boolean existsBySlugAndTenantId(String slug, UUID tenantId) {
        return jpa.existsBySlugAndTenantId(slug, tenantId);
    }

    @Override
    public boolean existsByCategoryIdAndTenantId(UUID categoryId, UUID tenantId) {
        return jpa.existsByCategoryIdAndTenantId(categoryId, tenantId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
