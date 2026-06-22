package com.loyalsuit.modules.settings.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.settings.application.NotificationSettingsService;
import com.loyalsuit.modules.settings.application.dto.NotificationPreferencesDto;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tenant notification settings. Store staff can view and update. */
@RestController
@RequestMapping("/api/v1/admin/settings/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification settings", description = "Tenant notification preferences")
public class AdminNotificationSettingsController {

    private final NotificationSettingsService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesDto>> get(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.get(principal.getTenantId())));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferencesDto>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationPreferencesDto request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(principal.getTenantId(), request)));
    }
}
