package com.loyalsuit.modules.marketplace.application.dto;

import com.loyalsuit.modules.marketplace.domain.Vendor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        UUID userId,
        String storeName,
        String slug,
        String description,
        String logoUrl,
        BigDecimal commissionRate,
        String status,
        Instant createdAt) {

    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getUserId(),
                vendor.getStoreName(),
                vendor.getSlug(),
                vendor.getDescription(),
                vendor.getLogoUrl(),
                vendor.getCommissionRate(),
                vendor.getStatus().name(),
                vendor.getCreatedAt());
    }
}
