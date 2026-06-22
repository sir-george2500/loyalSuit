package com.loyalsuit.modules.reviews.application.dto;

import com.loyalsuit.common.response.PageResponse;

/** The seller "Reviews" page: aggregate rating across their products plus the review list. */
public record VendorReviewsResponse(ReviewSummary summary, PageResponse<VendorReviewResponse> reviews) {}
