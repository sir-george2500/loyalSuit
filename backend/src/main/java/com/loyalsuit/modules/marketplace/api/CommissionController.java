package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.marketplace.application.CommissionService;
import com.loyalsuit.modules.marketplace.application.dto.CommissionEntryResponse;
import com.loyalsuit.modules.marketplace.application.dto.VendorEarningsResponse;
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

/**
 * Vendor self-service for earnings: a vendor reads their own commission ledger and
 * owed balance. Scoped to the caller's user id, so a vendor can only ever see their
 * own money.
 */
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
@Tag(name = "Vendor earnings", description = "A vendor's own commission ledger & balance")
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping("/earnings")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my earned balance and reversed total")
    public ResponseEntity<ApiResponse<VendorEarningsResponse>> earnings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                commissionService.earningsFor(principal.getTenantId(), principal.getUserId())));
    }

    @GetMapping("/commissions")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "List my commission ledger entries")
    public ResponseEntity<ApiResponse<PageResponse<CommissionEntryResponse>>> ledger(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                commissionService.listForVendor(principal.getTenantId(), principal.getUserId(), pageable)));
    }
}
