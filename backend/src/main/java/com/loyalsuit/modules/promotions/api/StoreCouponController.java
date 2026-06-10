package com.loyalsuit.modules.promotions.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.promotions.application.CouponService;
import com.loyalsuit.modules.promotions.application.dto.CouponPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public storefront coupon preview: what a code would take off the current cart. Lives
 * under {@code /store} (permitted for GET); the discount is computed from the server-priced
 * cart, and previewing never redeems the coupon.
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons (storefront)", description = "Public coupon preview")
public class StoreCouponController {

    private final CouponService couponService;

    @GetMapping("/{code}")
    @Operation(summary = "Preview a coupon against the current cart")
    public ResponseEntity<ApiResponse<CouponPreviewResponse>> preview(
            @PathVariable String storeSlug,
            @PathVariable String code,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.preview(storeSlug, code, cartToken)));
    }
}
