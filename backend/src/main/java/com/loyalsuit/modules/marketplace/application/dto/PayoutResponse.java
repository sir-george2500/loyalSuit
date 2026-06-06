package com.loyalsuit.modules.marketplace.application.dto;

import com.loyalsuit.modules.marketplace.domain.PayoutRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutResponse(
        UUID id,
        UUID vendorId,
        BigDecimal amount,
        String status,
        String reference,
        String resolutionNote,
        UUID decidedBy,
        Instant decidedAt,
        Instant createdAt) {

    public static PayoutResponse from(PayoutRequest p) {
        return new PayoutResponse(
                p.getId(),
                p.getVendorId(),
                p.getAmount(),
                p.getStatus().name(),
                p.getReference(),
                p.getResolutionNote(),
                p.getDecidedBy(),
                p.getDecidedAt(),
                p.getCreatedAt());
    }
}
