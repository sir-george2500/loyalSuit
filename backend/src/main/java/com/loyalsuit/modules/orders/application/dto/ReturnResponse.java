package com.loyalsuit.modules.orders.application.dto;

import com.loyalsuit.modules.orders.domain.ReturnRequest;

import java.time.Instant;
import java.util.UUID;

public record ReturnResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String status,
        String reason,
        String resolutionNote,
        Instant createdAt) {

    public static ReturnResponse from(ReturnRequest request) {
        return new ReturnResponse(
                request.getId(),
                request.getOrderId(),
                request.getOrderNumber(),
                request.getStatus().name(),
                request.getReason(),
                request.getResolutionNote(),
                request.getCreatedAt());
    }
}
