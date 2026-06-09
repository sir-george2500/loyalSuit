package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Product> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);

    @Query("""
            SELECT p FROM Product p
            WHERE p.tenantId = :tenantId AND p.status = com.loyalsuit.modules.catalog.domain.ProductStatus.ACTIVE
              AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.barcode) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Product> searchActive(@Param("tenantId") UUID tenantId, @Param("q") String q, Pageable pageable);
    Optional<Product> findBySlugAndTenantId(String slug, UUID tenantId);
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Product> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable);
    Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);
    Page<Product> findByTenantIdAndStatusAndCategoryId(
            UUID tenantId, ProductStatus status, UUID categoryId, Pageable pageable);
    Page<Product> findByCategoryIdAndTenantId(UUID categoryId, UUID tenantId, Pageable pageable);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
    boolean existsByCategoryIdAndTenantId(UUID categoryId, UUID tenantId);
}
