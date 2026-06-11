package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.hrm.application.AttendanceService;
import com.loyalsuit.modules.hrm.application.dto.AttendanceRequest;
import com.loyalsuit.modules.hrm.application.dto.AttendanceResponse;
import com.loyalsuit.modules.hrm.application.dto.TimesheetResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin HRM — attendance and timesheets. Owner-only (SUPER_ADMIN / TENANT_ADMIN); the service
 * also gates by subscription plan, so a BASIC tenant gets a 403 telling them to upgrade.
 */
@RestController
@RequestMapping("/api/v1/admin/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance (admin)", description = "HRM attendance and timesheets")
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Record (or correct) a day's attendance for an employee")
    public ResponseEntity<ApiResponse<AttendanceResponse>> record(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AttendanceRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.record(principal.getTenantId(), body)));
    }

    @PostMapping("/{employeeId}/clock-in")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Clock an employee in for today")
    public ResponseEntity<ApiResponse<AttendanceResponse>> clockIn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.clockIn(principal.getTenantId(), employeeId)));
    }

    @PostMapping("/{employeeId}/clock-out")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Clock an employee out for today")
    public ResponseEntity<ApiResponse<AttendanceResponse>> clockOut(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.clockOut(principal.getTenantId(), employeeId)));
    }

    @GetMapping("/timesheet")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "An employee's timesheet over a date range")
    public ResponseEntity<ApiResponse<TimesheetResponse>> timesheet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.timesheet(principal.getTenantId(), employeeId, from, to)));
    }
}
