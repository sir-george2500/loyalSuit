package com.loyalsuit.modules.tenants.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.tenants.application.OnboardingService;
import com.loyalsuit.modules.tenants.application.dto.CompleteOnboardingRequest;
import com.loyalsuit.modules.tenants.application.dto.OnboardingStatusResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "One-time tenant setup wizard")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Whether the current tenant has completed setup")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> status(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.getStatus(principal.getTenantId())));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Complete the tenant setup wizard (company, currency, first warehouse)")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> complete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteOnboardingRequest request) {
        OnboardingStatusResponse result =
                onboardingService.complete(principal.getTenantId(), principal.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.ok("Setup complete", result));
    }
}
