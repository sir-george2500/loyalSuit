package com.loyalsuit.modules.featureflags.infrastructure.persistence;

import com.loyalsuit.modules.featureflags.domain.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface FeatureFlagJpaRepository extends JpaRepository<FeatureFlag, UUID> {
    List<FeatureFlag> findAllByOrderByFlagKeyAsc();
    boolean existsByFlagKey(String flagKey);
}
