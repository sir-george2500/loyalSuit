package com.loyalsuit.modules.apikeys.infrastructure.persistence;

import com.loyalsuit.modules.apikeys.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ApiKeyJpaRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<ApiKey> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByKeyHash(String keyHash);
}
