package com.loyalsuit.modules.featureflags.domain.port;

import com.loyalsuit.modules.featureflags.domain.FeatureFlag;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository {
    List<FeatureFlag> findAllOrdered();
    Optional<FeatureFlag> findById(UUID id);
    boolean existsByFlagKey(String flagKey);
    FeatureFlag save(FeatureFlag flag);
    void deleteById(UUID id);
}
