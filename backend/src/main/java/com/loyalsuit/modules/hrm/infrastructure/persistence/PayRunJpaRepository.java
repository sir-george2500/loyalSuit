package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.PayRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

interface PayRunJpaRepository extends JpaRepository<PayRun, UUID> {
    Optional<PayRun> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<PayRun> findByTenantIdOrderByPeriodStartDesc(UUID tenantId, Pageable pageable);
    boolean existsByTenantIdAndPeriodStartAndPeriodEnd(UUID tenantId, LocalDate periodStart, LocalDate periodEnd);
}
