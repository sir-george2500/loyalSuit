package com.loyalsuit.modules.pos.application.dto;

import com.loyalsuit.modules.pos.domain.PosShift;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A cash-drawer session. Until it's closed the reconciliation fields (salesTotal,
 * expectedCash, countedCash, variance) are null.
 */
public record PosShiftResponse(
        UUID id,
        UUID cashierId,
        String status,
        BigDecimal openingFloat,
        Instant openedAt,
        Instant closedAt,
        BigDecimal salesTotal,
        int saleCount,
        BigDecimal expectedCash,
        BigDecimal countedCash,
        BigDecimal variance) {

    public static PosShiftResponse from(PosShift shift) {
        return new PosShiftResponse(
                shift.getId(),
                shift.getCashierId(),
                shift.getStatus().name(),
                shift.getOpeningFloat(),
                shift.getCreatedAt(),
                shift.getClosedAt(),
                shift.getSalesTotal(),
                shift.getSaleCount(),
                shift.getExpectedCash(),
                shift.getCountedCash(),
                shift.getVariance());
    }
}
