package com.loyalsuit.modules.fulfilment.infrastructure.shipping;

import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;
import com.loyalsuit.modules.fulfilment.domain.PickupPoint;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryZoneRepository;
import com.loyalsuit.modules.fulfilment.domain.port.PickupPointRepository;
import com.loyalsuit.modules.orders.application.port.ResolvedShippingZone;
import com.loyalsuit.modules.orders.application.port.ShippingZoneResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Fulfilment's implementation of the orders {@link ShippingZoneResolver} port: prices a
 * chosen zone at checkout. A zone is only honoured when both it and its pickup point are
 * active — a deactivated point's zones aren't offered, so they can't be charged either.
 */
@Component
@RequiredArgsConstructor
public class DeliveryZoneShippingResolver implements ShippingZoneResolver {

    private final DeliveryZoneRepository zoneRepository;
    private final PickupPointRepository pointRepository;

    @Override
    public Optional<ResolvedShippingZone> resolveActive(UUID tenantId, UUID zoneId) {
        return zoneRepository.findByIdAndTenantId(zoneId, tenantId)
                .filter(DeliveryZone::isActive)
                .flatMap(zone -> pointRepository.findByIdAndTenantId(zone.getPickupPointId(), tenantId)
                        .filter(PickupPoint::isActive)
                        .map(point -> new ResolvedShippingZone(zone.getName(), point.getName(), zone.getFee())));
    }
}
