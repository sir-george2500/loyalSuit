package com.loyalsuit.modules.fulfilment.application.dto;

import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;

import java.time.Instant;
import java.util.UUID;

/**
 * A delivery agent for the roster. {@code name} and {@code email} are resolved from the
 * linked user (null if that user can no longer be found).
 */
public record DeliveryAgentResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        String phone,
        String vehicleType,
        boolean active,
        Instant createdAt) {

    public static DeliveryAgentResponse from(DeliveryAgent agent, String name, String email) {
        return new DeliveryAgentResponse(
                agent.getId(),
                agent.getUserId(),
                name,
                email,
                agent.getPhone(),
                agent.getVehicleType().name(),
                agent.isActive(),
                agent.getCreatedAt());
    }
}
