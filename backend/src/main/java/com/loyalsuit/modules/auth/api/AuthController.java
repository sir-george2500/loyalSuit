package com.loyalsuit.modules.auth.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.auth.application.AuthService;
import com.loyalsuit.modules.auth.application.PasswordResetService;
import com.loyalsuit.modules.auth.application.dto.AuthResponse;
import com.loyalsuit.modules.auth.application.dto.ChangePasswordRequest;
import com.loyalsuit.modules.auth.application.dto.ForgotPasswordRequest;
import com.loyalsuit.modules.auth.application.dto.LoginRequest;
import com.loyalsuit.modules.auth.application.dto.RegisterRequest;
import com.loyalsuit.modules.auth.application.dto.ResetPasswordRequest;
import com.loyalsuit.modules.auth.application.dto.UserProfile;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Self-contained auth — the platform issues its own JWTs")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new business owner (creates a tenant + admin user)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfile>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(principal.getUserId())));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.message("Password updated successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link (always succeeds; no account enumeration)")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        // Same response whether or not the account exists — never reveal membership.
        return ResponseEntity.ok(ApiResponse.message(
                "If an account exists for that email, a reset link is on its way."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password using a reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.message("Your password has been reset. You can now sign in."));
    }
}
