package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.marketplace.application.PayoutService;
import com.loyalsuit.modules.marketplace.application.dto.PayPayoutRequest;
import com.loyalsuit.modules.marketplace.application.dto.PayoutResponse;
import com.loyalsuit.modules.marketplace.application.dto.RejectPayoutRequest;
import com.loyalsuit.modules.marketplace.domain.PayoutStatus;
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
 * Admin payout management. Disbursing funds is owner-only (SUPER_ADMIN / TENANT_ADMIN),
 * the same tier that approves vendors and reads the commission ledger. Pay/reject are
 * guarded terminal decisions, recorded in the audit trail.
 */
@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@Tag(name = "Payout management", description = "Admin review of vendor payout requests")
public class AdminPayoutController {

    private final PayoutService payoutService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List payout requests (optionally by status)")
    public ResponseEntity<ApiResponse<PageResponse<PayoutResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(payoutService.list(principal.getTenantId(), status, pageable)));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Mark a payout request as paid")
    public ResponseEntity<ApiResponse<PayoutResponse>> pay(
            @PathVariable UUID id,
            @Valid @RequestBody PayPayoutRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(payoutService.pay(
                id, principal.getTenantId(), actor(principal), body.getReference(), body.getNote())));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Reject a payout request")
    public ResponseEntity<ApiResponse<PayoutResponse>> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectPayoutRequest body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(payoutService.reject(
                id, principal.getTenantId(), actor(principal), body.getNote())));
    }

    private static AuditActor actor(UserPrincipal principal) {
        return AuditActor.of(principal.getTenantId(), principal.getUserId(),
                principal.getEmail(), principal.getRole());
    }
}
