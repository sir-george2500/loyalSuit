package com.loyalsuit.modules.affiliate.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.affiliate.application.AffiliateRewardService;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateMeResponse;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateRewardResponse;
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
 * An affiliate's own view: their referral code, reward rate, earnings, and ledger. Open to
 * any authenticated user — the service 404s if the caller isn't actually an affiliate, so a
 * dedicated AFFILIATE role isn't needed.
 */
@RestController
@RequestMapping("/api/v1/affiliate")
@RequiredArgsConstructor
@Tag(name = "Affiliate", description = "An affiliate's own code, earnings & ledger")
public class AffiliateController {

    private final AffiliateRewardService rewardService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "My affiliate code, rate and earnings")
    public ResponseEntity<ApiResponse<AffiliateMeResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                rewardService.me(principal.getTenantId(), principal.getUserId())));
    }

    @GetMapping("/me/rewards")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "My reward ledger (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<AffiliateRewardResponse>>> rewards(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                rewardService.myRewards(principal.getTenantId(), principal.getUserId(), pageable)));
    }
}
