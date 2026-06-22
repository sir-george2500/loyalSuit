package com.loyalsuit.modules.reviews.application.dto;

import com.loyalsuit.modules.reviews.domain.Review;

import java.time.Instant;
import java.util.UUID;

/** A single review as shown on a product page (no customer identity beyond the display name). */
public record ReviewResponse(
        UUID id,
        int rating,
        String comment,
        String authorName,
        Instant createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getRating(),
                review.getComment(),
                review.getAuthorName(),
                review.getCreatedAt());
    }
}
