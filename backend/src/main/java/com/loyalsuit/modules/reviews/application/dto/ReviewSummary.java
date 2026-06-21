package com.loyalsuit.modules.reviews.application.dto;

import com.loyalsuit.modules.reviews.domain.ReviewStats;

/** Aggregate rating: the mean (rounded to one decimal) and the number of reviews. */
public record ReviewSummary(double averageRating, long count) {

    public static ReviewSummary from(ReviewStats stats) {
        double rounded = Math.round(stats.average() * 10.0) / 10.0;
        return new ReviewSummary(rounded, stats.count());
    }
}
