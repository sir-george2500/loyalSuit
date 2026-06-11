package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.hrm.application.PayrollService;
import com.loyalsuit.modules.hrm.application.dto.PayRunCreate;
import com.loyalsuit.modules.hrm.application.dto.PayRunDetailResponse;
import com.loyalsuit.modules.hrm.application.dto.PayRunResponse;
import com.loyalsuit.modules.hrm.application.dto.PayslipAdjust;
import com.loyalsuit.modules.hrm.application.dto.PayslipResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin HRM — payroll: pay runs and their payslips. Owner-only (SUPER_ADMIN / TENANT_ADMIN);
 * the service also gates by subscription plan, so a BASIC tenant gets a 403 to upgrade.
 */
@RestController
@RequestMapping("/api/v1/admin/payroll")
@RequiredArgsConstructor
@Tag(name = "Payroll (admin)", description = "HRM pay runs and payslips")
public class AdminPayrollController {

    private final PayrollService payrollService;

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List pay runs")
    public ResponseEntity<ApiResponse<PageResponse<PayRunResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.list(principal.getTenantId(), pageable)));
    }

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Generate a draft pay run for a period")
    public ResponseEntity<ApiResponse<PayRunDetailResponse>> generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PayRunCreate body) {
        PayRunDetailResponse run = payrollService.generate(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(run));
    }

    @GetMapping("/runs/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "A pay run with its payslips")
    public ResponseEntity<ApiResponse<PayRunDetailResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.get(principal.getTenantId(), id)));
    }

    @PostMapping("/runs/{id}/finalise")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Lock a draft pay run")
    public ResponseEntity<ApiResponse<PayRunResponse>> finalise(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.finalise(principal.getTenantId(), id)));
    }

    @PostMapping("/runs/{id}/pay")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Mark a finalised pay run as paid")
    public ResponseEntity<ApiResponse<PayRunResponse>> pay(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.pay(principal.getTenantId(), id)));
    }

    @PutMapping("/payslips/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Adjust a draft payslip's deductions")
    public ResponseEntity<ApiResponse<PayslipResponse>> adjust(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PayslipAdjust body) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.adjust(principal.getTenantId(), id, body)));
    }
}
