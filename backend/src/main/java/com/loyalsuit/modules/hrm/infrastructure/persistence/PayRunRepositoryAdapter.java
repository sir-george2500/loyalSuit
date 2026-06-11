package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.PayRun;
import com.loyalsuit.modules.hrm.domain.port.PayRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PayRunRepositoryAdapter implements PayRunRepository {

    private final PayRunJpaRepository jpa;

    @Override
    public PayRun save(PayRun payRun) {
        return jpa.save(payRun);
    }

    @Override
    public Optional<PayRun> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Page<PayRun> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByPeriodStartDesc(tenantId, pageable);
    }

    @Override
    public boolean existsForPeriod(UUID tenantId, LocalDate periodStart, LocalDate periodEnd) {
        return jpa.existsByTenantIdAndPeriodStartAndPeriodEnd(tenantId, periodStart, periodEnd);
    }
}
