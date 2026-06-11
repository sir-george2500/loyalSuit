package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Payslip;
import com.loyalsuit.modules.hrm.domain.port.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PayslipRepositoryAdapter implements PayslipRepository {

    private final PayslipJpaRepository jpa;

    @Override
    public Payslip save(Payslip payslip) {
        return jpa.save(payslip);
    }

    @Override
    public List<Payslip> saveAll(List<Payslip> payslips) {
        return jpa.saveAll(payslips);
    }

    @Override
    public Optional<Payslip> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Payslip> findByPayRunId(UUID payRunId) {
        return jpa.findByPayRunIdOrderByEmployeeNameAsc(payRunId);
    }
}
