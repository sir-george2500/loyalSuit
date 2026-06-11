package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.LeaveType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveTypeRepository {
    LeaveType save(LeaveType leaveType);
    Optional<LeaveType> findByIdAndTenantId(UUID id, UUID tenantId);
    List<LeaveType> findByTenantId(UUID tenantId);
    List<LeaveType> findActiveByTenantId(UUID tenantId);
}
