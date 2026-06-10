package com.loyalsuit.modules.orders.application.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Prices a chosen delivery zone for checkout. Defined here (in orders) and implemented by
 * the fulfilment module — dependency inversion, so orders never depends on fulfilment and
 * there is no cycle (fulfilment already depends on orders).
 */
public interface ShippingZoneResolver {

    /** The zone's pricing, only if it (and its pickup point) is active in the tenant. */
    Optional<ResolvedShippingZone> resolveActive(UUID tenantId, UUID zoneId);
}
