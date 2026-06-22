package com.loyalsuit.modules.featureflags.infrastructure.persistence;

import com.loyalsuit.modules.featureflags.domain.FeatureFlag;
import com.loyalsuit.modules.featureflags.domain.port.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FeatureFlagRepositoryAdapter implements FeatureFlagRepository {

    private final FeatureFlagJpaRepository jpa;

    @Override
    public List<FeatureFlag> findAllOrdered() {
        return jpa.findAllByOrderByFlagKeyAsc();
    }

    @Override
    public Optional<FeatureFlag> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsByFlagKey(String flagKey) {
        return jpa.existsByFlagKey(flagKey);
    }

    @Override
    public FeatureFlag save(FeatureFlag flag) {
        return jpa.save(flag);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
