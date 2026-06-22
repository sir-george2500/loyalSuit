package com.loyalsuit.modules.featureflags.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.featureflags.application.dto.FeatureFlagResponse;
import com.loyalsuit.modules.featureflags.application.dto.UpsertFeatureFlagRequest;
import com.loyalsuit.modules.featureflags.domain.FeatureFlag;
import com.loyalsuit.modules.featureflags.domain.port.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Manages global platform feature flags (platform owner only). */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository repository;

    @Transactional(readOnly = true)
    public List<FeatureFlagResponse> list() {
        return repository.findAllOrdered().stream().map(FeatureFlagService::toResponse).toList();
    }

    @Transactional
    public FeatureFlagResponse create(UpsertFeatureFlagRequest request) {
        String key = request.getFlagKey().trim();
        if (repository.existsByFlagKey(key)) {
            throw new ConflictException("A flag with key '" + key + "' already exists.");
        }
        FeatureFlag flag = new FeatureFlag();
        flag.setFlagKey(key);
        flag.setDescription(trimToNull(request.getDescription()));
        flag.setEnabled(request.isEnabled());
        return toResponse(repository.save(flag));
    }

    @Transactional
    public FeatureFlagResponse update(UUID id, UpsertFeatureFlagRequest request) {
        FeatureFlag flag = require(id);
        String key = request.getFlagKey().trim();
        if (!flag.getFlagKey().equals(key) && repository.existsByFlagKey(key)) {
            throw new ConflictException("A flag with key '" + key + "' already exists.");
        }
        flag.setFlagKey(key);
        flag.setDescription(trimToNull(request.getDescription()));
        flag.setEnabled(request.isEnabled());
        return toResponse(repository.save(flag));
    }

    @Transactional
    public FeatureFlagResponse setEnabled(UUID id, boolean enabled) {
        FeatureFlag flag = require(id);
        flag.setEnabled(enabled);
        return toResponse(repository.save(flag));
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(require(id).getId());
    }

    private FeatureFlag require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Feature flag", id));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static FeatureFlagResponse toResponse(FeatureFlag f) {
        return new FeatureFlagResponse(f.getId(), f.getFlagKey(), f.getDescription(), f.isEnabled(), f.getCreatedAt());
    }
}
