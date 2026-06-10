package com.loyalsuit.modules.affiliate.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.affiliate.application.AffiliateService;
import com.loyalsuit.modules.affiliate.application.dto.AffiliateResponse;
import com.loyalsuit.modules.affiliate.application.dto.RegisterAffiliateRequest;
import com.loyalsuit.modules.affiliate.application.dto.UpdateAffiliateRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin management of affiliates. Affiliate rewards are a payout obligation, so it's
 * owner-only (SUPER_ADMIN / TENANT_ADMIN).
 */
@RestController
@RequestMapping("/api/v1/admin/affiliates")
@RequiredArgsConstructor
@Tag(name = "Affiliates (admin)", description = "Referral program management")
public class AdminAffiliateController {

    private final AffiliateService affiliateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List affiliates")
    public ResponseEntity<ApiResponse<PageResponse<AffiliateResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(affiliateService.list(principal.getTenantId(), pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Enrol a user as an affiliate")
    public ResponseEntity<ApiResponse<AffiliateResponse>> register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterAffiliateRequest body) {
        AffiliateResponse affiliate = affiliateService.register(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(affiliate));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update an affiliate's code or reward rate")
    public ResponseEntity<ApiResponse<AffiliateResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateAffiliateRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(affiliateService.update(id, principal.getTenantId(), body)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Activate an affiliate")
    public ResponseEntity<ApiResponse<AffiliateResponse>> activate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(affiliateService.setActive(id, principal.getTenantId(), true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Deactivate an affiliate")
    public ResponseEntity<ApiResponse<AffiliateResponse>> deactivate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(affiliateService.setActive(id, principal.getTenantId(), false)));
    }
}
