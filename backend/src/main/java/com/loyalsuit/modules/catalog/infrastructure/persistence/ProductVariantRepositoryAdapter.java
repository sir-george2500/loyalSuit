package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.ProductVariant;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductVariantRepositoryAdapter implements ProductVariantRepository {

    private final ProductVariantJpaRepository jpa;

    @Override
    public ProductVariant save(ProductVariant variant) {
        return jpa.save(variant);
    }

    @Override
    public Optional<ProductVariant> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<ProductVariant> findByProductId(UUID productId) {
        return jpa.findByProductId(productId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
