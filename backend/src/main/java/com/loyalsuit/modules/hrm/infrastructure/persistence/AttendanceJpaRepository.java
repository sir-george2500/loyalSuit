package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AttendanceJpaRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Attendance> findByTenantIdAndEmployeeIdAndWorkDate(UUID tenantId, UUID employeeId, LocalDate workDate);

    List<Attendance> findByTenantIdAndEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(
            UUID tenantId, UUID employeeId, LocalDate from, LocalDate to);
}
