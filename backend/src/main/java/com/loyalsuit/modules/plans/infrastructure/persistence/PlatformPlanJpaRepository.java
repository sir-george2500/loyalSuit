package com.loyalsuit.modules.plans.infrastructure.persistence;

import com.loyalsuit.modules.plans.domain.PlatformPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PlatformPlanJpaRepository extends JpaRepository<PlatformPlan, UUID> {
    List<PlatformPlan> findAllByOrderByPriceAsc();
    boolean existsByCode(String code);
}
