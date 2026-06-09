package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryEventRepositoryAdapter implements DeliveryEventRepository {

    private final DeliveryEventJpaRepository jpa;

    @Override
    public DeliveryEvent save(DeliveryEvent event) {
        return jpa.save(event);
    }

    @Override
    public List<DeliveryEvent> findByDeliveryIdAndTenantId(UUID deliveryId, UUID tenantId) {
        return jpa.findByDeliveryIdAndTenantIdOrderByCreatedAtAsc(deliveryId, tenantId);
    }
}
