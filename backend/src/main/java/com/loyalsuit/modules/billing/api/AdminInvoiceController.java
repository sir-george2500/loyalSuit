package com.loyalsuit.modules.billing.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.billing.application.BillingService;
import com.loyalsuit.modules.billing.application.dto.InvoiceResponse;
import com.loyalsuit.modules.billing.application.dto.InvoiceSummaryResponse;
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

/** Invoices (rendered from orders). Owner-level, read-only. */
@RestController
@RequestMapping("/api/v1/admin/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Order invoices")
public class AdminInvoiceController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List invoices")
    public ResponseEntity<ApiResponse<PageResponse<InvoiceSummaryResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listInvoices(principal.getTenantId(), pageable)));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get a single invoice with line items")
    public ResponseEntity<ApiResponse<InvoiceResponse>> get(
            @PathVariable UUID orderId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getInvoice(principal.getTenantId(), orderId)));
    }
}
