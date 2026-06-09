package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DeliveryEventJpaRepository extends JpaRepository<DeliveryEvent, UUID> {
    List<DeliveryEvent> findByDeliveryIdAndTenantIdOrderByCreatedAtAsc(UUID deliveryId, UUID tenantId);
}
