package com.loyalsuit.modules.platform.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.platform.application.PlatformTenantService;
import com.loyalsuit.modules.platform.application.dto.ChangePlanRequest;
import com.loyalsuit.modules.platform.application.dto.TenantAdminResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Platform tenant directory. Restricted to the platform owner (SUPER_ADMIN). */
@RestController
@RequestMapping("/api/v1/platform/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Platform tenants", description = "Cross-tenant administration")
public class PlatformTenantController {

    private final PlatformTenantService service;

    @GetMapping
    @Operation(summary = "List all tenants")
    public ResponseEntity<ApiResponse<PageResponse<TenantAdminResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(pageable)));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a tenant")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setActive(id, true)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a tenant")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setActive(id, false)));
    }

    @PatchMapping("/{id}/plan")
    @Operation(summary = "Change a tenant's subscription plan")
    public ResponseEntity<ApiResponse<TenantAdminResponse>> changePlan(
            @PathVariable UUID id, @Valid @RequestBody ChangePlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.changePlan(id, request.plan())));
    }
}
