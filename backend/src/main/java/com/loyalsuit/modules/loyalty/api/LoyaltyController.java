package com.loyalsuit.modules.loyalty.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.loyalty.application.LoyaltyService;
import com.loyalsuit.modules.loyalty.application.dto.LoyaltyBalanceResponse;
import com.loyalsuit.modules.loyalty.application.dto.LoyaltyTransactionResponse;
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
 * A customer's own loyalty: their points balance and the ledger behind it. Scoped to the
 * authenticated customer; points accrue on their paid orders.
 */
@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "A customer's points balance & history")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "My points balance")
    public ResponseEntity<ApiResponse<LoyaltyBalanceResponse>> balance(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                loyaltyService.balanceFor(principal.getTenantId(), principal.getUserId())));
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "My points history (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<LoyaltyTransactionResponse>>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                loyaltyService.history(principal.getTenantId(), principal.getUserId(), pageable)));
    }
}
