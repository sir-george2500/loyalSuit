package com.loyalsuit.modules.reviews.infrastructure.persistence;

import com.loyalsuit.modules.reviews.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface ReviewJpaRepository extends JpaRepository<Review, UUID> {

    boolean existsByTenantIdAndCustomerIdAndProductId(UUID tenantId, UUID customerId, UUID productId);

    Page<Review> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    long countByProductId(UUID productId);

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM Review r WHERE r.productId = :productId")
    long sumRatingByProduct(@Param("productId") UUID productId);

    Page<Review> findByTenantIdAndVendorIdOrderByCreatedAtDesc(UUID tenantId, UUID vendorId, Pageable pageable);

    long countByTenantIdAndVendorId(UUID tenantId, UUID vendorId);

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM Review r WHERE r.tenantId = :tenantId AND r.vendorId = :vendorId")
    long sumRatingByVendor(@Param("tenantId") UUID tenantId, @Param("vendorId") UUID vendorId);
}
