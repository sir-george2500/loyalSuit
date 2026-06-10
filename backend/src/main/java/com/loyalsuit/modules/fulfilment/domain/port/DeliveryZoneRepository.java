package com.loyalsuit.modules.fulfilment.domain.port;

import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneRepository {
    DeliveryZone save(DeliveryZone zone);
    Optional<DeliveryZone> findByIdAndTenantId(UUID id, UUID tenantId);
    void delete(DeliveryZone zone);
    /** All zones of a point (admin view of one point). */
    List<DeliveryZone> findByPickupPointIdAndTenantId(UUID pickupPointId, UUID tenantId);
    /** Every zone in the tenant (to group under points without an N+1). */
    List<DeliveryZone> findByTenantId(UUID tenantId);
    /** Active zones only, for the storefront and checkout. */
    List<DeliveryZone> findActiveByTenantId(UUID tenantId);
}
