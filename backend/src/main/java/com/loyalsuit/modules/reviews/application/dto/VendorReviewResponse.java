package com.loyalsuit.modules.reviews.application.dto;

import com.loyalsuit.modules.reviews.domain.Review;

import java.time.Instant;
import java.util.UUID;

/** A review in the seller's "Reviews" list — adds which product it was left on. */
public record VendorReviewResponse(
        UUID id,
        UUID productId,
        String productName,
        int rating,
        String comment,
        String authorName,
        Instant createdAt) {

    public static VendorReviewResponse from(Review review, String productName) {
        return new VendorReviewResponse(
                review.getId(),
                review.getProductId(),
                productName,
                review.getRating(),
                review.getComment(),
                review.getAuthorName(),
                review.getCreatedAt());
    }
}
