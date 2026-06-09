package com.loyalsuit.modules.fulfilment.domain.port;

import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryAgentRepository {
    DeliveryAgent save(DeliveryAgent agent);
    Optional<DeliveryAgent> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<DeliveryAgent> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    Page<DeliveryAgent> findByTenantId(UUID tenantId, Pageable pageable);
    Page<DeliveryAgent> findByTenantIdAndActive(UUID tenantId, boolean active, Pageable pageable);
}
