package com.loyalsuit.modules.apikeys.application.dto;

import java.time.Instant;
import java.util.UUID;

/** An API key for display — never includes the secret. */
public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        String lastFour,
        boolean active,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt) {}
