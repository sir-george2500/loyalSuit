package com.loyalsuit.modules.catalog.infrastructure.persistence;

import com.loyalsuit.modules.catalog.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ProductVariantJpaRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(UUID productId);
}
