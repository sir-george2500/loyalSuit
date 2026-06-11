package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository {
    LeaveRequest save(LeaveRequest request);
    Optional<LeaveRequest> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<LeaveRequest> findByTenantId(UUID tenantId, Pageable pageable);
    Page<LeaveRequest> findByTenantIdAndStatus(UUID tenantId, LeaveStatus status, Pageable pageable);
    List<LeaveRequest> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);

    /** Approved leave for one employee whose range starts within [from, to] — used for balances. */
    List<LeaveRequest> findApproved(UUID tenantId, UUID employeeId, LocalDate from, LocalDate to);
}
