package com.loyalsuit.modules.catalog.domain.port;

import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Product> findBySlugAndTenantId(String slug, UUID tenantId);
    Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);
    /** Active products whose name, SKU, or barcode matches the query (case-insensitive). */
    Page<Product> searchActive(UUID tenantId, String query, Pageable pageable);
    Page<Product> findByTenantIdAndStatusAndCategoryId(
            UUID tenantId, ProductStatus status, UUID categoryId, Pageable pageable);
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Product> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable);
    Page<Product> findByCategoryIdAndTenantId(UUID categoryId, UUID tenantId, Pageable pageable);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
    boolean existsByCategoryIdAndTenantId(UUID categoryId, UUID tenantId);
    void deleteById(UUID id);
}
