package com.loyalsuit.modules.reviews.domain;

/**
 * Aggregate rating for a product or a vendor: the mean rating and how many reviews it is
 * based on. {@code average} is 0 when {@code count} is 0.
 */
public record ReviewStats(double average, long count) {

    public static final ReviewStats EMPTY = new ReviewStats(0.0, 0L);
}
