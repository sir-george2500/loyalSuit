package com.loyalsuit.modules.reviews.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.marketplace.application.VendorService;
import com.loyalsuit.modules.reviews.application.ReviewService;
import com.loyalsuit.modules.reviews.application.dto.VendorReviewsResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Seller "Reviews": reviews left on the acting vendor's products. The caller's vendor id (the
 * Vendor PK) is resolved from their account, so a seller only ever sees their own reviews.
 */
@RestController
@RequestMapping("/api/v1/vendor/reviews")
@RequiredArgsConstructor
@Tag(name = "Vendor", description = "Seller reviews")
public class VendorReviewController {

    private final ReviewService reviewService;
    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Reviews on my products, with my aggregate rating")
    public ResponseEntity<ApiResponse<VendorReviewsResponse>> myReviews(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID vendorId = vendorService.requireVendorId(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.forVendor(principal.getTenantId(), vendorId, pageable)));
    }
}
