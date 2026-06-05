package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);
    Page<Product> findByCategoryIdAndTenantId(UUID categoryId, UUID tenantId, Pageable pageable);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
    boolean existsByCategoryIdAndTenantId(UUID categoryId, UUID tenantId);
}
