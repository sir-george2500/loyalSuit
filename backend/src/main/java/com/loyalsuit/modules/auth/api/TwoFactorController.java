package com.loyalsuit.modules.auth.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.auth.application.TwoFactorService;
import com.loyalsuit.modules.auth.application.dto.TwoFactorCodeRequest;
import com.loyalsuit.modules.auth.application.dto.TwoFactorDisableRequest;
import com.loyalsuit.modules.auth.application.dto.TwoFactorEnableResponse;
import com.loyalsuit.modules.auth.application.dto.TwoFactorSetupResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Two-factor auth management for the signed-in user: start enrolment, confirm it, and disable.
 * Authenticated (these are not in the public auth allow-list); the login challenge itself is
 * completed via {@code POST /api/v1/auth/login/2fa}.
 */
@RestController
@RequestMapping("/api/v1/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-factor auth", description = "TOTP enrolment and management")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @PostMapping("/setup")
    @Operation(summary = "Start 2FA enrolment — returns a secret and otpauth URI for a QR code")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(twoFactorService.setup(principal.getUserId())));
    }

    @PostMapping("/enable")
    @Operation(summary = "Confirm enrolment with a code — turns 2FA on and returns recovery codes")
    public ResponseEntity<ApiResponse<TwoFactorEnableResponse>> enable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                twoFactorService.enable(principal.getUserId(), request.getCode().trim())));
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable 2FA (re-authenticates with the account password)")
    public ResponseEntity<ApiResponse<Void>> disable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        twoFactorService.disable(principal.getUserId(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.message("Two-factor authentication disabled"));
    }
}
