package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A delivery. {@code events} is the status timeline — populated on the single-delivery
 * view and null in list responses.
 */
public record DeliveryResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String customerName,
        UUID agentId,
        String agentName,
        String status,
        Instant assignedAt,
        Instant pickedUpAt,
        Instant deliveredAt,
        Instant failedAt,
        String failureReason,
        Instant createdAt,
        List<DeliveryEventResponse> events) {

    public static DeliveryResponse from(Delivery d) {
        return build(d, null);
    }

    public static DeliveryResponse withTimeline(Delivery d, List<DeliveryEvent> events) {
        return build(d, events.stream().map(DeliveryEventResponse::from).toList());
    }

    private static DeliveryResponse build(Delivery d, List<DeliveryEventResponse> events) {
        return new DeliveryResponse(
                d.getId(),
                d.getOrderId(),
                d.getOrderNumber(),
                d.getCustomerName(),
                d.getAgentId(),
                d.getAgentName(),
                d.getStatus().name(),
                d.getAssignedAt(),
                d.getPickedUpAt(),
                d.getDeliveredAt(),
                d.getFailedAt(),
                d.getFailureReason(),
                d.getCreatedAt(),
                events);
    }
}
