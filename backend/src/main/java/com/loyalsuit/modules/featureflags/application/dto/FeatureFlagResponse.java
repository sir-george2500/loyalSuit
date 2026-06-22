package com.loyalsuit.modules.featureflags.application.dto;

import java.time.Instant;
import java.util.UUID;

public record FeatureFlagResponse(
        UUID id,
        String flagKey,
        String description,
        boolean enabled,
        Instant createdAt) {}
