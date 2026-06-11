package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.hrm.application.dto.AttendanceRequest;
import com.loyalsuit.modules.hrm.application.dto.AttendanceResponse;
import com.loyalsuit.modules.hrm.application.dto.TimesheetResponse;
import com.loyalsuit.modules.hrm.domain.Attendance;
import com.loyalsuit.modules.hrm.domain.AttendanceStatus;
import com.loyalsuit.modules.hrm.domain.port.AttendanceRepository;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Employee attendance. Records are admin-entered or stamped by clock-in / clock-out, one per
 * employee per day. Every operation is plan-gated via {@link HrmAccess} and tenant-scoped, and
 * confirms the employee belongs to the tenant before touching their attendance.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final HrmAccess hrmAccess;

    /** Create or overwrite the attendance record for an employee on a given day. */
    @Transactional
    public AttendanceResponse record(UUID tenantId, AttendanceRequest request) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, request.getEmployeeId());
        if (request.getCheckIn() != null && request.getCheckOut() != null
                && !request.getCheckOut().isAfter(request.getCheckIn())) {
            throw new BusinessException("Check-out must be after check-in", HttpStatus.BAD_REQUEST);
        }

        Attendance attendance = attendanceRepository
                .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, request.getEmployeeId(), request.getWorkDate())
                .orElseGet(() -> new Attendance(tenantId, request.getEmployeeId(), request.getWorkDate(), request.getStatus()));
        attendance.setStatus(request.getStatus());
        attendance.setCheckIn(request.getCheckIn());
        attendance.setCheckOut(request.getCheckOut());
        attendance.setNote(StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null);
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    /** Stamp the employee in for today. Fails if they are already clocked in. */
    @Transactional
    public AttendanceResponse clockIn(UUID tenantId, UUID employeeId) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, employeeId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Attendance attendance = attendanceRepository
                .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, today)
                .orElseGet(() -> new Attendance(tenantId, employeeId, today, AttendanceStatus.PRESENT));
        if (attendance.getCheckIn() != null) {
            throw new BusinessException("Already clocked in today", HttpStatus.CONFLICT);
        }
        attendance.setCheckIn(Instant.now());
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    /** Stamp the employee out for today. Fails if they never clocked in or already clocked out. */
    @Transactional
    public AttendanceResponse clockOut(UUID tenantId, UUID employeeId) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, employeeId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Attendance attendance = attendanceRepository
                .findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, today)
                .orElseThrow(() -> new BusinessException("Not clocked in today", HttpStatus.CONFLICT));
        if (attendance.getCheckIn() == null) {
            throw new BusinessException("Not clocked in today", HttpStatus.CONFLICT);
        }
        if (attendance.getCheckOut() != null) {
            throw new BusinessException("Already clocked out today", HttpStatus.CONFLICT);
        }
        attendance.setCheckOut(Instant.now());
        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    /** An employee's attendance over an inclusive date range, with total hours. */
    public TimesheetResponse timesheet(UUID tenantId, UUID employeeId, LocalDate from, LocalDate to) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, employeeId);
        if (to.isBefore(from)) {
            throw new BusinessException("The end date can't be before the start date", HttpStatus.BAD_REQUEST);
        }
        return TimesheetResponse.of(employeeId, from, to,
                attendanceRepository.findTimesheet(tenantId, employeeId, from, to));
    }

    private void requireEmployee(UUID tenantId, UUID employeeId) {
        employeeRepository.findByIdAndTenantId(employeeId, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", employeeId));
    }
}
