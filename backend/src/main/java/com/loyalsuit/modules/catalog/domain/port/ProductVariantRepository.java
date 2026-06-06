package com.loyalsuit.modules.catalog.domain.port;

import com.loyalsuit.modules.catalog.domain.ProductVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository {
    ProductVariant save(ProductVariant variant);
    Optional<ProductVariant> findById(UUID id);
    List<ProductVariant> findByProductId(UUID productId);
    void deleteById(UUID id);
}
