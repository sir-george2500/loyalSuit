package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.marketplace.application.PayoutService;
import com.loyalsuit.modules.marketplace.application.dto.PayoutBalanceResponse;
import com.loyalsuit.modules.marketplace.application.dto.PayoutResponse;
import com.loyalsuit.modules.marketplace.application.dto.RequestPayoutRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vendor self-service for payouts: a vendor sees their payout standing, requests a
 * payout against their available balance, and lists their own requests. Everything is
 * scoped to the caller's user id (VENDOR-only).
 */
@RestController
@RequestMapping("/api/v1/vendor/payouts")
@RequiredArgsConstructor
@Tag(name = "Vendor payouts", description = "A vendor's own payout requests & balance")
public class VendorPayoutController {

    private final PayoutService payoutService;

    @GetMapping("/balance")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my payout balance (earned, pending, paid, available)")
    public ResponseEntity<ApiResponse<PayoutBalanceResponse>> balance(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                payoutService.balanceFor(principal.getTenantId(), principal.getUserId())));
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Request a payout against my available balance")
    public ResponseEntity<ApiResponse<PayoutResponse>> request(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RequestPayoutRequest body) {
        PayoutResponse response = payoutService.request(principal.getTenantId(), actor(principal), body.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "List my payout requests")
    public ResponseEntity<ApiResponse<PageResponse<PayoutResponse>>> mine(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                payoutService.listMine(principal.getTenantId(), principal.getUserId(), pageable)));
    }

    private static AuditActor actor(UserPrincipal principal) {
        return AuditActor.of(principal.getTenantId(), principal.getUserId(),
                principal.getEmail(), principal.getRole());
    }
}
