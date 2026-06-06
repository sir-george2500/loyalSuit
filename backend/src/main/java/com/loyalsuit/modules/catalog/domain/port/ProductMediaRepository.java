package com.loyalsuit.modules.catalog.domain.port;

import com.loyalsuit.modules.catalog.domain.ProductMedia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMediaRepository {
    ProductMedia save(ProductMedia media);
    Optional<ProductMedia> findById(UUID id);
    List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId);
    /** Primary images for a batch of products — one query, no N+1 in listings. */
    List<ProductMedia> findByProductIdInAndPrimaryTrue(List<UUID> productIds);
    int countByProductId(UUID productId);
    void deleteById(UUID id);
}
