package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryAgentRepositoryAdapter implements DeliveryAgentRepository {

    private final DeliveryAgentJpaRepository jpa;

    @Override
    public DeliveryAgent save(DeliveryAgent agent) {
        return jpa.save(agent);
    }

    @Override
    public Optional<DeliveryAgent> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<DeliveryAgent> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpa.existsByUserId(userId);
    }

    @Override
    public Page<DeliveryAgent> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<DeliveryAgent> findByTenantIdAndActive(UUID tenantId, boolean active, Pageable pageable) {
        return jpa.findByTenantIdAndActiveOrderByCreatedAtDesc(tenantId, active, pageable);
    }
}
