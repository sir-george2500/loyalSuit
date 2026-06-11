package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
import com.loyalsuit.modules.hrm.domain.port.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LeaveRequestRepositoryAdapter implements LeaveRequestRepository {

    private final LeaveRequestJpaRepository jpa;

    @Override
    public LeaveRequest save(LeaveRequest request) {
        return jpa.save(request);
    }

    @Override
    public Optional<LeaveRequest> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Page<LeaveRequest> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<LeaveRequest> findByTenantIdAndStatus(UUID tenantId, LeaveStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }

    @Override
    public List<LeaveRequest> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId) {
        return jpa.findByTenantIdAndEmployeeIdOrderByStartDateDesc(tenantId, employeeId);
    }

    @Override
    public List<LeaveRequest> findApproved(UUID tenantId, UUID employeeId, LocalDate from, LocalDate to) {
        return jpa.findByTenantIdAndEmployeeIdAndStatusAndStartDateBetween(
                tenantId, employeeId, LeaveStatus.APPROVED, from, to);
    }
}
