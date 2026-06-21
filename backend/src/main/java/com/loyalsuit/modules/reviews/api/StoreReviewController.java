package com.loyalsuit.modules.reviews.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.reviews.application.ReviewService;
import com.loyalsuit.modules.reviews.application.dto.ProductReviewsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public product reviews. Keyed by the globally-unique product id, so no store slug or auth is
 * needed; {@code GET /api/v1/store/**} is permitted anonymously by SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/store/products/{productId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Storefront", description = "Public product reviews")
public class StoreReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "A product's reviews and aggregate rating")
    public ResponseEntity<ApiResponse<ProductReviewsResponse>> reviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.forProduct(productId, pageable)));
    }
}
