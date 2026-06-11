package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.hrm.application.EmployeeService;
import com.loyalsuit.modules.hrm.application.dto.EmployeeRequest;
import com.loyalsuit.modules.hrm.application.dto.EmployeeResponse;
import com.loyalsuit.modules.hrm.domain.EmployeeStatus;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin HRM — the employee roster. Owner-only (SUPER_ADMIN / TENANT_ADMIN); the service
 * also gates by subscription plan, so a BASIC tenant gets a 403 telling them to upgrade.
 */
@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Employees (admin)", description = "HRM employee roster")
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List employees (optionally by status)")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) EmployeeStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.list(principal.getTenantId(), status, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Add an employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeRequest body) {
        EmployeeResponse employee = employeeService.create(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(employee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update an employee")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmployeeRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(id, principal.getTenantId(), body)));
    }
}
