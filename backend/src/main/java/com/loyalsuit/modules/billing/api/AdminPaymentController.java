package com.loyalsuit.modules.billing.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.billing.application.BillingService;
import com.loyalsuit.modules.billing.application.dto.PaymentResponse;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Payments ledger (derived from orders). Owner-level, read-only. */
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Order payment ledger")
public class AdminPaymentController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List payments (optionally by status)")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listPayments(principal.getTenantId(), status, pageable)));
    }
}
