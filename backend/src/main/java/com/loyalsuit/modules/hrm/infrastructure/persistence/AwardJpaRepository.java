package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Award;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AwardJpaRepository extends JpaRepository<Award, UUID> {
    Optional<Award> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Award> findByTenantIdAndEmployeeIdOrderByAwardedOnDesc(UUID tenantId, UUID employeeId);
}
