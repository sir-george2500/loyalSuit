package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;

import java.time.Instant;
import java.util.UUID;

/** One entry on a delivery's status timeline. */
public record DeliveryEventResponse(
        String status,
        String note,
        UUID actorId,
        Instant occurredAt) {

    public static DeliveryEventResponse from(DeliveryEvent event) {
        return new DeliveryEventResponse(
                event.getStatus().name(),
                event.getNote(),
                event.getActorId(),
                event.getCreatedAt());
    }
}
