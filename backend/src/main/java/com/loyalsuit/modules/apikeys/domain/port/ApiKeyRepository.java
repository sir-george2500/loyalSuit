package com.loyalsuit.modules.apikeys.domain.port;

import com.loyalsuit.modules.apikeys.domain.ApiKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository {
    List<ApiKey> findByTenantId(UUID tenantId);
    Optional<ApiKey> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByKeyHash(String keyHash);
    ApiKey save(ApiKey apiKey);
}
