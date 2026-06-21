package com.loyalsuit.modules.reviews.domain.port;

import com.loyalsuit.modules.reviews.domain.Review;
import com.loyalsuit.modules.reviews.domain.ReviewStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReviewRepository {

    Review save(Review review);

    boolean existsByTenantIdAndCustomerIdAndProductId(UUID tenantId, UUID customerId, UUID productId);

    /** Public product page: a product's reviews, newest first. */
    Page<Review> findByProductId(UUID productId, Pageable pageable);

    /** Aggregate rating for a product (across tenants the productId is unique). */
    ReviewStats statsForProduct(UUID productId);

    /** Seller view: reviews of a vendor's products within the tenant, newest first. */
    Page<Review> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable);

    /** Aggregate rating across a vendor's products. */
    ReviewStats statsForVendor(UUID tenantId, UUID vendorId);
}
