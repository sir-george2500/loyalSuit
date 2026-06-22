package com.loyalsuit.modules.plans.domain.port;

import com.loyalsuit.modules.plans.domain.PlatformPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlatformPlanRepository {
    List<PlatformPlan> findAllOrdered();
    Optional<PlatformPlan> findById(UUID id);
    boolean existsByCode(String code);
    PlatformPlan save(PlatformPlan plan);
}
