package com.loyalsuit.modules.promotions.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.promotions.application.CouponService;
import com.loyalsuit.modules.promotions.application.dto.CouponRequest;
import com.loyalsuit.modules.promotions.application.dto.CouponResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin coupon management. Coupons set prices, so it's owner-only (SUPER_ADMIN / TENANT_ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons (admin)", description = "Discount code management")
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List coupons (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.list(principal.getTenantId(), pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CouponRequest body) {
        CouponResponse coupon = couponService.create(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(coupon));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CouponRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.update(id, principal.getTenantId(), body)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Activate a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> activate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.setActive(id, principal.getTenantId(), true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Deactivate a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> deactivate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.setActive(id, principal.getTenantId(), false)));
    }
}
