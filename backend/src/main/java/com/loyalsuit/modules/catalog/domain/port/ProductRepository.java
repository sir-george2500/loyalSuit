package com.loyalsuit.modules.catalog.domain.port;

import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
    /** Batch lookup by id within a tenant — one query for many ids (e.g. receipt lines). */
    List<Product> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);
    Optional<Product> findBySlugAndTenantId(String slug, UUID tenantId);
    Page<Product> findByTenantIdAndStatus(UUID tenantId, ProductStatus status, Pageable pageable);
    /** Active products whose name, SKU, or barcode matches the query (case-insensitive). */
    Page<Product> searchActive(UUID tenantId, String query, Pageable pageable);

    // --- Public marketplace feed: ACTIVE products that are either house products (no vendor) or
    //     sold by an ACTIVE vendor. The caller supplies the visible vendor ids, so the catalog
    //     never reaches into the vendors table. House products sort first. ---
    Page<Product> findVisibleActiveHouseFirst(UUID tenantId, Collection<UUID> visibleVendorIds, Pageable pageable);
    Page<Product> findVisibleActiveByCategory(
            UUID tenantId, UUID categoryId, Collection<UUID> visibleVendorIds, Pageable pageable);
    Page<Product> searchVisibleActive(
            UUID tenantId, String query, Collection<UUID> visibleVendorIds, Pageable pageable);
    Page<Product> findByTenantIdAndStatusAndCategoryId(
            UUID tenantId, ProductStatus status, UUID categoryId, Pageable pageable);
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Product> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable);
    /** A vendor's publicly visible (ACTIVE) products, for their storefront page. */
    Page<Product> findByTenantIdAndVendorIdAndStatus(
            UUID tenantId, UUID vendorId, ProductStatus status, Pageable pageable);
    Page<Product> findByCategoryIdAndTenantId(UUID categoryId, UUID tenantId, Pageable pageable);
    boolean existsBySlugAndTenantId(String slug, UUID tenantId);
    boolean existsByCategoryIdAndTenantId(UUID categoryId, UUID tenantId);
    /** Active-product counts keyed by tenant id, for the marketplace index (one grouped query). */
    Map<UUID, Long> countActiveProductsByTenant(Collection<UUID> tenantIds);
    void deleteById(UUID id);
}
