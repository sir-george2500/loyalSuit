package com.loyalsuit.modules.marketplace.application.dto;

import com.loyalsuit.modules.marketplace.domain.CommissionEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionEntryResponse(
        UUID id,
        UUID vendorId,
        UUID orderId,
        UUID orderItemId,
        String orderNumber,
        BigDecimal grossAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        String status,
        Instant createdAt) {

    public static CommissionEntryResponse from(CommissionEntry e) {
        return new CommissionEntryResponse(
                e.getId(),
                e.getVendorId(),
                e.getOrderId(),
                e.getOrderItemId(),
                e.getOrderNumber(),
                e.getGrossAmount(),
                e.getCommissionRate(),
                e.getCommissionAmount(),
                e.getNetAmount(),
                e.getStatus().name(),
                e.getCreatedAt());
    }
}
