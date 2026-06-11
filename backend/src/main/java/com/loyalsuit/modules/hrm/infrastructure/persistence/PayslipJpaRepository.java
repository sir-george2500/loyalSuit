package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PayslipJpaRepository extends JpaRepository<Payslip, UUID> {
    Optional<Payslip> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Payslip> findByPayRunIdOrderByEmployeeNameAsc(UUID payRunId);
}
