package com.loyalsuit.modules.apikeys.infrastructure.persistence;

import com.loyalsuit.modules.apikeys.domain.ApiKey;
import com.loyalsuit.modules.apikeys.domain.port.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final ApiKeyJpaRepository jpa;

    @Override
    public List<ApiKey> findByTenantId(UUID tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Override
    public Optional<ApiKey> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public boolean existsByKeyHash(String keyHash) {
        return jpa.existsByKeyHash(keyHash);
    }

    @Override
    public ApiKey save(ApiKey apiKey) {
        return jpa.save(apiKey);
    }
}
