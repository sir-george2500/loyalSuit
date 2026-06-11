package com.loyalsuit.modules.privacy.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.privacy.application.DataPrivacyService;
import com.loyalsuit.modules.privacy.application.dto.DeleteAccountRequest;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GDPR data-subject rights for the signed-in user: download a copy of their data, or delete
 * (anonymise) their account. Authenticated and self-scoped — a user only ever acts on themselves.
 */
@RestController
@RequestMapping("/api/v1/privacy")
@RequiredArgsConstructor
@Tag(name = "Privacy", description = "GDPR data export and account deletion")
public class DataPrivacyController {

    private final DataPrivacyService dataPrivacyService;

    @GetMapping("/export")
    @Operation(summary = "Export a copy of your personal data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> export(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                dataPrivacyService.export(principal.getUserId(), principal.getTenantId())));
    }

    @PostMapping("/delete-account")
    @Operation(summary = "Permanently delete (anonymise) your account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request) {
        dataPrivacyService.deleteAccount(principal.getUserId(), principal.getTenantId(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.message("Your account has been deleted"));
    }
}
