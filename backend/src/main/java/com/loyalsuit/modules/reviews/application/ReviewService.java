package com.loyalsuit.modules.reviews.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.application.PurchaseQueryService;
import com.loyalsuit.modules.orders.application.dto.PurchasedItem;
import com.loyalsuit.modules.reviews.application.dto.CreateReviewRequest;
import com.loyalsuit.modules.reviews.application.dto.ProductReviewsResponse;
import com.loyalsuit.modules.reviews.application.dto.ReviewEligibility;
import com.loyalsuit.modules.reviews.application.dto.ReviewResponse;
import com.loyalsuit.modules.reviews.application.dto.ReviewSummary;
import com.loyalsuit.modules.reviews.application.dto.VendorReviewResponse;
import com.loyalsuit.modules.reviews.application.dto.VendorReviewsResponse;
import com.loyalsuit.modules.reviews.domain.Review;
import com.loyalsuit.modules.reviews.domain.port.ReviewRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product reviews. A customer may review a product only after a delivered purchase (verified
 * via {@link PurchaseQueryService}), once per product. Reviews feed the public product page
 * and the seller's "Reviews" view. The selling vendor is snapshotted from the purchase so the
 * seller view never joins back to orders.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PurchaseQueryService purchaseQueryService;
    private final AppUserRepository appUserRepository;
    private final ProductRepository productRepository;

    @Transactional
    public ReviewResponse create(UUID tenantId, UUID customerId, CreateReviewRequest request) {
        UUID productId = request.getProductId();
        if (reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId)) {
            throw new ConflictException("You've already reviewed this product");
        }
        PurchasedItem purchase = purchaseQueryService.findDeliveredPurchase(tenantId, customerId, productId)
                .orElseThrow(() -> new BusinessException(
                        "You can review a product only after an order containing it is delivered"));

        AppUser author = appUserRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("User", customerId));

        Review review = new Review(tenantId, productId, customerId, purchase.vendorId(),
                displayName(author.getFullName()), request.getRating(), trimToNull(request.getComment()));
        return ReviewResponse.from(reviewRepository.save(review));
    }

    public ReviewEligibility eligibility(UUID tenantId, UUID customerId, UUID productId) {
        boolean alreadyReviewed =
                reviewRepository.existsByTenantIdAndCustomerIdAndProductId(tenantId, customerId, productId);
        boolean purchased = purchaseQueryService.findDeliveredPurchase(tenantId, customerId, productId).isPresent();
        return new ReviewEligibility(!alreadyReviewed && purchased, alreadyReviewed);
    }

    public ProductReviewsResponse forProduct(UUID productId, Pageable pageable) {
        Page<ReviewResponse> page = reviewRepository.findByProductId(productId, pageable).map(ReviewResponse::from);
        ReviewSummary summary = ReviewSummary.from(reviewRepository.statsForProduct(productId));
        return new ProductReviewsResponse(summary, new PageResponse<>(page));
    }

    public VendorReviewsResponse forVendor(UUID tenantId, UUID vendorId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByTenantIdAndVendorId(tenantId, vendorId, pageable);
        Map<UUID, String> productNames = productNames(tenantId, page.getContent());
        Page<VendorReviewResponse> mapped = page.map(r ->
                VendorReviewResponse.from(r, productNames.getOrDefault(r.getProductId(), "Product")));
        ReviewSummary summary = ReviewSummary.from(reviewRepository.statsForVendor(tenantId, vendorId));
        return new VendorReviewsResponse(summary, new PageResponse<>(mapped));
    }

    private Map<UUID, String> productNames(UUID tenantId, List<Review> reviews) {
        List<UUID> ids = reviews.stream().map(Review::getProductId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findByIdInAndTenantId(ids, tenantId).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    /** Privacy-minimised display name: "Jane Doe" -> "Jane D."; single name kept; blank -> "Customer". */
    private static String displayName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Customer";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        String last = parts[parts.length - 1];
        return parts[0] + " " + Character.toUpperCase(last.charAt(0)) + ".";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
