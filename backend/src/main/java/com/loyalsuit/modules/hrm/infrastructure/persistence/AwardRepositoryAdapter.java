package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Award;
import com.loyalsuit.modules.hrm.domain.port.AwardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AwardRepositoryAdapter implements AwardRepository {

    private final AwardJpaRepository jpa;

    @Override
    public Award save(Award award) {
        return jpa.save(award);
    }

    @Override
    public Optional<Award> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Award> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId) {
        return jpa.findByTenantIdAndEmployeeIdOrderByAwardedOnDesc(tenantId, employeeId);
    }

    @Override
    public void delete(Award award) {
        jpa.delete(award);
    }
}
