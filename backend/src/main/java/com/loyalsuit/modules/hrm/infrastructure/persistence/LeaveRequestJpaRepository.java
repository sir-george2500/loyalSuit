package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LeaveRequestJpaRepository extends JpaRepository<LeaveRequest, UUID> {
    Optional<LeaveRequest> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<LeaveRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<LeaveRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, LeaveStatus status, Pageable pageable);

    List<LeaveRequest> findByTenantIdAndEmployeeIdOrderByStartDateDesc(UUID tenantId, UUID employeeId);

    List<LeaveRequest> findByTenantIdAndEmployeeIdAndStatusAndStartDateBetween(
            UUID tenantId, UUID employeeId, LeaveStatus status, LocalDate from, LocalDate to);

    List<LeaveRequest> findByTenantIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID tenantId, LeaveStatus status, LocalDate periodEnd, LocalDate periodStart);
}
