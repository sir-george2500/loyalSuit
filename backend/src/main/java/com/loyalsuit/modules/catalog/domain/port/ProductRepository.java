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
    Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Product> findByCategoryIdAndTenantId(UUID categoryId, UUID tenantId, Pageable pageable);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
    void deleteById(UUID id);
}
