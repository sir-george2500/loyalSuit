package com.loyalsuit.modules.staff.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.staff.application.StaffService;
import com.loyalsuit.modules.staff.application.dto.ChangeRoleRequest;
import com.loyalsuit.modules.staff.application.dto.StaffResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Staff & Roles management. Assigning roles and disabling accounts is an owner-level
 * action (SUPER_ADMIN / TENANT_ADMIN), not available to general staff.
 */
@RestController
@RequestMapping("/api/v1/admin/staff")
@RequiredArgsConstructor
@Tag(name = "Staff & Roles", description = "Tenant team management")
public class AdminStaffController {

    private final StaffService staffService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List staff members")
    public ResponseEntity<ApiResponse<PageResponse<StaffResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.list(principal.getTenantId(), pageable)));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Change a staff member's role")
    public ResponseEntity<ApiResponse<StaffResponse>> changeRole(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                staffService.changeRole(principal.getTenantId(), principal.getUserId(), id, request.getRole())));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Reactivate a staff account")
    public ResponseEntity<ApiResponse<StaffResponse>> activate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                staffService.setActive(principal.getTenantId(), principal.getUserId(), id, true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Deactivate a staff account")
    public ResponseEntity<ApiResponse<StaffResponse>> deactivate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                staffService.setActive(principal.getTenantId(), principal.getUserId(), id, false)));
    }
}
