package com.loyalsuit.modules.apikeys.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.apikeys.application.ApiKeyService;
import com.loyalsuit.modules.apikeys.application.dto.ApiKeyResponse;
import com.loyalsuit.modules.apikeys.application.dto.CreateApiKeyRequest;
import com.loyalsuit.modules.apikeys.application.dto.CreatedApiKeyResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Tenant API key management. Owner-level (SUPER_ADMIN / TENANT_ADMIN). */
@RestController
@RequestMapping("/api/v1/admin/api-keys")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
@Tag(name = "API keys", description = "Tenant API key management")
public class AdminApiKeyController {

    private final ApiKeyService service;

    @GetMapping
    @Operation(summary = "List API keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(principal.getTenantId())));
    }

    @PostMapping
    @Operation(summary = "Create an API key (returns the secret once)")
    public ResponseEntity<ApiResponse<CreatedApiKeyResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateApiKeyRequest request) {
        CreatedApiKeyResponse created =
                service.create(principal.getTenantId(), principal.getUserId(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> revoke(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(service.revoke(principal.getTenantId(), id)));
    }
}
