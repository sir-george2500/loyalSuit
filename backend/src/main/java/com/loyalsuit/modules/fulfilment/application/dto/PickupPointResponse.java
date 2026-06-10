package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;
import com.loyalsuit.modules.fulfilment.domain.PickupPoint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A pickup point with the zones it serves (each point defines its own zones). */
public record PickupPointResponse(
        UUID id,
        String name,
        String address,
        String phone,
        boolean active,
        List<DeliveryZoneResponse> zones,
        Instant createdAt) {

    public static PickupPointResponse from(PickupPoint point, List<DeliveryZone> zones) {
        return new PickupPointResponse(
                point.getId(),
                point.getName(),
                point.getAddress(),
                point.getPhone(),
                point.isActive(),
                zones.stream().map(DeliveryZoneResponse::from).toList(),
                point.getCreatedAt());
    }
}
