package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface LeaveTypeJpaRepository extends JpaRepository<LeaveType, UUID> {
    Optional<LeaveType> findByIdAndTenantId(UUID id, UUID tenantId);
    List<LeaveType> findByTenantIdOrderByNameAsc(UUID tenantId);
    List<LeaveType> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);
}
