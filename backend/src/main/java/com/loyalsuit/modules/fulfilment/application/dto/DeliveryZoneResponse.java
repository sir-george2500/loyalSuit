package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryZoneResponse(
        UUID id,
        UUID pickupPointId,
        String name,
        BigDecimal fee,
        boolean active) {

    public static DeliveryZoneResponse from(DeliveryZone zone) {
        return new DeliveryZoneResponse(
                zone.getId(), zone.getPickupPointId(), zone.getName(), zone.getFee(), zone.isActive());
    }
}
