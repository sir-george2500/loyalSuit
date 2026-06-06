package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductMediaRepositoryAdapter implements ProductMediaRepository {

    private final ProductMediaJpaRepository jpa;

    @Override
    public ProductMedia save(ProductMedia media) {
        return jpa.save(media);
    }

    @Override
    public Optional<ProductMedia> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId) {
        return jpa.findByProductIdOrderBySortOrderAsc(productId);
    }

    @Override
    public List<ProductMedia> findByProductIdInAndPrimaryTrue(List<UUID> productIds) {
        return productIds.isEmpty() ? List.of() : jpa.findByProductIdInAndPrimaryTrue(productIds);
    }

    @Override
    public int countByProductId(UUID productId) {
        return jpa.countByProductId(productId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
