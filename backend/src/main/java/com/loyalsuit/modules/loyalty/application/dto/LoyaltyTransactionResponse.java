package com.loyalsuit.modules.loyalty.application.dto;

import com.loyalsuit.modules.loyalty.domain.LoyaltyTransaction;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyTransactionResponse(
        String type,
        int points,
        UUID orderId,
        Instant occurredAt) {

    public static LoyaltyTransactionResponse from(LoyaltyTransaction tx) {
        return new LoyaltyTransactionResponse(tx.getType().name(), tx.getPoints(), tx.getOrderId(), tx.getCreatedAt());
    }
}
