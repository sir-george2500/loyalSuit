package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryAdapter implements DeliveryRepository {

    private final DeliveryJpaRepository jpa;

    @Override
    public Delivery save(Delivery delivery) {
        return jpa.save(delivery);
    }

    @Override
    public Optional<Delivery> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<Delivery> findByOrderIdAndTenantId(UUID orderId, UUID tenantId) {
        return jpa.findByOrderIdAndTenantId(orderId, tenantId);
    }

    @Override
    public Page<Delivery> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<Delivery> findByTenantIdAndStatus(UUID tenantId, DeliveryStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }
}
