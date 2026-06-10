package com.loyalsuit.modules.fulfilment.domain.port;

import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository {
    Delivery save(Delivery delivery);
    Optional<Delivery> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Delivery> findByOrderIdAndTenantId(UUID orderId, UUID tenantId);
    Page<Delivery> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Delivery> findByTenantIdAndStatus(UUID tenantId, DeliveryStatus status, Pageable pageable);
    /** An agent's still-open deliveries (ASSIGNED / PICKED_UP / IN_TRANSIT) — their work queue. */
    Page<Delivery> findActiveByAgent(UUID tenantId, UUID agentId, Pageable pageable);
}
