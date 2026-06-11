package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.hrm.application.LeaveTypeService;
import com.loyalsuit.modules.hrm.application.dto.LeaveTypeRequest;
import com.loyalsuit.modules.hrm.application.dto.LeaveTypeResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin HRM — leave types. Owner-only (SUPER_ADMIN / TENANT_ADMIN); the service also gates by
 * subscription plan, so a BASIC tenant gets a 403 telling them to upgrade.
 */
@RestController
@RequestMapping("/api/v1/admin/leave-types")
@RequiredArgsConstructor
@Tag(name = "Leave types (admin)", description = "HRM leave categories and allowances")
public class AdminLeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List leave types")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(leaveTypeService.list(principal.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Add a leave type")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LeaveTypeRequest body) {
        LeaveTypeResponse type = leaveTypeService.create(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(type));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update a leave type")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LeaveTypeRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(leaveTypeService.update(id, principal.getTenantId(), body)));
    }
}
