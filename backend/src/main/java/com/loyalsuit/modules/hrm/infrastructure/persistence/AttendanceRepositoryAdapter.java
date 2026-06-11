package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Attendance;
import com.loyalsuit.modules.hrm.domain.port.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AttendanceRepositoryAdapter implements AttendanceRepository {

    private final AttendanceJpaRepository jpa;

    @Override
    public Attendance save(Attendance attendance) {
        return jpa.save(attendance);
    }

    @Override
    public Optional<Attendance> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<Attendance> findByTenantIdAndEmployeeIdAndWorkDate(UUID tenantId, UUID employeeId, LocalDate workDate) {
        return jpa.findByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, workDate);
    }

    @Override
    public List<Attendance> findTimesheet(UUID tenantId, UUID employeeId, LocalDate from, LocalDate to) {
        return jpa.findByTenantIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(tenantId, employeeId, from, to);
    }
}
