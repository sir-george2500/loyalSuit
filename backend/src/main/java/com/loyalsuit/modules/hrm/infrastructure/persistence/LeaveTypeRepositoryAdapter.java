package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LeaveTypeRepositoryAdapter implements LeaveTypeRepository {

    private final LeaveTypeJpaRepository jpa;

    @Override
    public LeaveType save(LeaveType leaveType) {
        return jpa.save(leaveType);
    }

    @Override
    public Optional<LeaveType> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<LeaveType> findByTenantId(UUID tenantId) {
        return jpa.findByTenantIdOrderByNameAsc(tenantId);
    }

    @Override
    public List<LeaveType> findActiveByTenantId(UUID tenantId) {
        return jpa.findByTenantIdAndActiveTrueOrderByNameAsc(tenantId);
    }
}
