package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository {
    Attendance save(Attendance attendance);
    Optional<Attendance> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Attendance> findByTenantIdAndEmployeeIdAndWorkDate(UUID tenantId, UUID employeeId, LocalDate workDate);
    List<Attendance> findTimesheet(UUID tenantId, UUID employeeId, LocalDate from, LocalDate to);
}
