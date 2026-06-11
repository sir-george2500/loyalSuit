package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.Payslip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayslipRepository {
    Payslip save(Payslip payslip);
    List<Payslip> saveAll(List<Payslip> payslips);
    Optional<Payslip> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Payslip> findByPayRunId(UUID payRunId);
}
