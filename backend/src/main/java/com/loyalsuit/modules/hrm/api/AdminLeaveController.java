package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.hrm.application.LeaveService;
import com.loyalsuit.modules.hrm.application.dto.LeaveBalanceResponse;
import com.loyalsuit.modules.hrm.application.dto.LeaveDecision;
import com.loyalsuit.modules.hrm.application.dto.LeaveRequestCreate;
import com.loyalsuit.modules.hrm.application.dto.LeaveRequestResponse;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin HRM — leave requests and balances. Owner-only (SUPER_ADMIN / TENANT_ADMIN); the service
 * also gates by subscription plan, so a BASIC tenant gets a 403 telling them to upgrade.
 */
@RestController
@RequestMapping("/api/v1/admin/leave")
@RequiredArgsConstructor
@Tag(name = "Leave (admin)", description = "HRM leave requests and balances")
public class AdminLeaveController {

    private final LeaveService leaveService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List leave requests (optionally by status)")
    public ResponseEntity<ApiResponse<PageResponse<LeaveRequestResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.list(principal.getTenantId(), status, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Raise a leave request for an employee")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LeaveRequestCreate body) {
        LeaveRequestResponse request = leaveService.create(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Approve a pending leave request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) LeaveDecision body) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.approve(principal.getTenantId(), id, body)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Reject a pending leave request")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody(required = false) LeaveDecision body) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.reject(principal.getTenantId(), id, body)));
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "An employee's leave balances for the year")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> balances(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.balances(principal.getTenantId(), employeeId)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "An employee's leave history")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.history(principal.getTenantId(), employeeId)));
    }
}
