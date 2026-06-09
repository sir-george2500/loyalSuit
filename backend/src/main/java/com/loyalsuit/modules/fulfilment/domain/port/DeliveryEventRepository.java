package com.loyalsuit.modules.fulfilment.domain.port;

import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;

import java.util.List;
import java.util.UUID;

public interface DeliveryEventRepository {
    DeliveryEvent save(DeliveryEvent event);
    /** A delivery's timeline, oldest first. */
    List<DeliveryEvent> findByDeliveryIdAndTenantId(UUID deliveryId, UUID tenantId);
}
