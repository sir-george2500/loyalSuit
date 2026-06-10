package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;

import java.time.Instant;
import java.util.List;

/**
 * The customer-facing view of a delivery — deliberately sanitized: just the status and a
 * timeline of when each stage happened, plus who signed for it. Internal notes, the
 * actor, the agent, and failure reasons are NOT exposed to the public tracking endpoint.
 *
 * @param status {@code PREPARING} until the order is dispatched, then the delivery status.
 */
public record DeliveryTrackingResponse(
        String orderNumber,
        String status,
        String recipientName,
        List<Stage> timeline) {

    public record Stage(String status, Instant occurredAt) {
    }

    /** An order that has no delivery yet (placed, not dispatched). */
    public static DeliveryTrackingResponse preparing(String orderNumber) {
        return new DeliveryTrackingResponse(orderNumber, "PREPARING", null, List.of());
    }

    public static DeliveryTrackingResponse from(Delivery delivery, List<DeliveryEvent> events) {
        List<Stage> timeline = events.stream()
                .map(e -> new Stage(e.getStatus().name(), e.getCreatedAt()))
                .toList();
        return new DeliveryTrackingResponse(
                delivery.getOrderNumber(),
                delivery.getStatus().name(),
                delivery.getRecipientName(),
                timeline);
    }
}
