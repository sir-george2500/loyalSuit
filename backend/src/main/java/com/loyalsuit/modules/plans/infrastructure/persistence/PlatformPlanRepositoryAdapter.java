package com.loyalsuit.modules.plans.infrastructure.persistence;

import com.loyalsuit.modules.plans.domain.PlatformPlan;
import com.loyalsuit.modules.plans.domain.port.PlatformPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PlatformPlanRepositoryAdapter implements PlatformPlanRepository {

    private final PlatformPlanJpaRepository jpa;

    @Override
    public List<PlatformPlan> findAllOrdered() {
        return jpa.findAllByOrderByPriceAsc();
    }

    @Override
    public Optional<PlatformPlan> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public PlatformPlan save(PlatformPlan plan) {
        return jpa.save(plan);
    }
}
