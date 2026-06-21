package com.loyalsuit.modules.reviews.application.dto;

/**
 * Whether the signed-in customer may review a product. {@code canReview} is true only when
 * they have a delivered purchase and haven't reviewed it yet; {@code alreadyReviewed} lets
 * the UI show their existing review instead of the form.
 */
public record ReviewEligibility(boolean canReview, boolean alreadyReviewed) {}
