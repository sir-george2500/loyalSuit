package com.loyalsuit.modules.reviews.application.dto;

import com.loyalsuit.common.response.PageResponse;

/** A product's reviews page bundled with its aggregate rating, for the public product page. */
public record ProductReviewsResponse(ReviewSummary summary, PageResponse<ReviewResponse> reviews) {}
