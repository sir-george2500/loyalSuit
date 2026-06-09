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
}
