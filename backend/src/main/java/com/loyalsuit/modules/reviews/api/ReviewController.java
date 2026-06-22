package com.loyalsuit.modules.reviews.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.reviews.application.ReviewService;
import com.loyalsuit.modules.reviews.application.dto.CreateReviewRequest;
import com.loyalsuit.modules.reviews.application.dto.ReviewEligibility;
import com.loyalsuit.modules.reviews.application.dto.ReviewResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer-facing review actions. Writing a review is gated server-side by a delivered
 * purchase; the eligibility check lets the storefront show the form only when it would succeed.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Customer product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Write a review for a product I've received")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.create(principal.getTenantId(), principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Can I review this product? (delivered purchase, not already reviewed)")
    public ResponseEntity<ApiResponse<ReviewEligibility>> eligibility(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID productId) {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.eligibility(principal.getTenantId(), principal.getUserId(), productId)));
    }
}
