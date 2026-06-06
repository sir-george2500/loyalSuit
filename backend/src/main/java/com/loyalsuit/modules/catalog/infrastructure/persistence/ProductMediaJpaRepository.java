package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ProductMediaJpaRepository extends JpaRepository<ProductMedia, UUID> {
    List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId);
    List<ProductMedia> findByProductIdInAndPrimaryTrue(List<UUID> productIds);
    int countByProductId(UUID productId);
}
