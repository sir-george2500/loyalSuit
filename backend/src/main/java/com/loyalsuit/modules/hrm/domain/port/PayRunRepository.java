package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.PayRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PayRunRepository {
    PayRun save(PayRun payRun);
    Optional<PayRun> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<PayRun> findByTenantId(UUID tenantId, Pageable pageable);
    boolean existsForPeriod(UUID tenantId, LocalDate periodStart, LocalDate periodEnd);
}
