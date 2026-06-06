package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.marketplace.application.CommissionService;
import com.loyalsuit.modules.marketplace.application.dto.CommissionEntryResponse;
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
 * Admin view of the commission ledger. Financial data, so it is restricted to tenant
 * owners (SUPER_ADMIN / TENANT_ADMIN) — the same tier that manages vendors and rates.
 */
@RestController
@RequestMapping("/api/v1/admin/commissions")
@RequiredArgsConstructor
@Tag(name = "Commission ledger", description = "Tenant-wide commission ledger (admin)")
public class AdminCommissionController {

    private final CommissionService commissionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List the tenant's commission ledger")
    public ResponseEntity<ApiResponse<PageResponse<CommissionEntryResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                commissionService.listForTenant(principal.getTenantId(), pageable)));
    }
}
