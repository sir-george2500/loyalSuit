package com.loyalsuit.modules.reviews.infrastructure.persistence;

import com.loyalsuit.modules.reviews.domain.Review;
import com.loyalsuit.modules.reviews.domain.ReviewStats;
import com.loyalsuit.modules.reviews.domain.port.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewJpaRepository jpa;

    @Override
    public Review save(Review review) {
        return jpa.save(review);
    }

    @Override
    public boolean existsByTenantIdAndCustomerIdAndProductId(UUID tenantId, UUID customerId, UUID productId) {
        return jpa.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId);
    }

    @Override
    public Page<Review> findByProductId(UUID productId, Pageable pageable) {
        return jpa.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    }

    @Override
    public ReviewStats statsForProduct(UUID productId) {
        long count = jpa.countByProductId(productId);
        return stats(count, count == 0 ? 0L : jpa.sumRatingByProduct(productId));
    }

    @Override
    public Page<Review> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable) {
        return jpa.findByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId, pageable);
    }

    @Override
    public ReviewStats statsForVendor(UUID tenantId, UUID vendorId) {
        long count = jpa.countByTenantIdAndVendorId(tenantId, vendorId);
        return stats(count, count == 0 ? 0L : jpa.sumRatingByVendor(tenantId, vendorId));
    }

    /** Average is computed in Java to keep the DB query robust across dialects (no AVG cast). */
    private static ReviewStats stats(long count, long sum) {
        return count == 0 ? ReviewStats.EMPTY : new ReviewStats((double) sum / count, count);
    }
}
