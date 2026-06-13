package com.loyalsuit.modules.orders.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.marketplace.application.VendorService;
import com.loyalsuit.modules.orders.application.VendorOrderService;
import com.loyalsuit.modules.orders.application.dto.VendorOrderResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * A seller's own orders — the orders that contain their products, showing only their lines and
 * earnings. VENDOR-only; the acting vendor is resolved from the principal, never trusted from input.
 */
@RestController
@RequestMapping("/api/v1/vendor/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
@Tag(name = "Vendor Orders", description = "A seller's orders (their lines only)")
public class VendorOrderController {

    private final VendorOrderService vendorOrderService;
    private final VendorService vendorService;

    @GetMapping
    @Operation(summary = "List orders that contain my products")
    public ResponseEntity<ApiResponse<PageResponse<VendorOrderResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID vendorId = vendorService.requireVendorId(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                vendorOrderService.list(principal.getTenantId(), vendorId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my orders (my lines only)")
    public ResponseEntity<ApiResponse<VendorOrderResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID vendorId = vendorService.requireVendorId(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(
                vendorOrderService.get(principal.getTenantId(), vendorId, id)));
    }
}
