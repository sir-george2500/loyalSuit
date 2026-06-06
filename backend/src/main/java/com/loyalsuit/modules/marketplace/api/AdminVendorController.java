package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.marketplace.application.VendorService;
import com.loyalsuit.modules.marketplace.application.dto.CommissionRequest;
import com.loyalsuit.modules.marketplace.application.dto.VendorResponse;
import com.loyalsuit.modules.marketplace.domain.VendorStatus;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin vendor management. Approval grants the VENDOR role, so it is restricted to
 * tenant owners (SUPER_ADMIN / TENANT_ADMIN), not general staff.
 */
@RestController
@RequestMapping("/api/v1/admin/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendor management", description = "Admin approval & vendor lifecycle")
public class AdminVendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List vendors (optionally by status)")
    public ResponseEntity<ApiResponse<PageResponse<VendorResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) VendorStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.list(principal.getTenantId(), status, pageable)));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Approve a vendor (grants the VENDOR role)")
    public ResponseEntity<ApiResponse<VendorResponse>> approve(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.approve(id, principal.getTenantId())));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Reject a vendor application")
    public ResponseEntity<ApiResponse<VendorResponse>> reject(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.reject(id, principal.getTenantId())));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Suspend an active vendor")
    public ResponseEntity<ApiResponse<VendorResponse>> suspend(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.suspend(id, principal.getTenantId())));
    }

    @PatchMapping("/{id}/reinstate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Reinstate a suspended vendor")
    public ResponseEntity<ApiResponse<VendorResponse>> reinstate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.reinstate(id, principal.getTenantId())));
    }

    @PatchMapping("/{id}/commission")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Set a vendor's commission rate")
    public ResponseEntity<ApiResponse<VendorResponse>> setCommission(
            @PathVariable UUID id,
            @Valid @RequestBody CommissionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                vendorService.setCommission(id, principal.getTenantId(), request.getRate())));
    }
}
