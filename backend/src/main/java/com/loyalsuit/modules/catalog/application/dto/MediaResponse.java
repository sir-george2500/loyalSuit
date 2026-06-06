package com.loyalsuit.modules.catalog.application.dto;

import com.loyalsuit.modules.catalog.domain.ProductMedia;

import java.util.UUID;

public record MediaResponse(
        UUID id,
        UUID productId,
        String url,
        boolean primary,
        int sortOrder) {

    public static MediaResponse from(ProductMedia media) {
        return new MediaResponse(
                media.getId(),
                media.getProductId(),
                media.getUrl(),
                media.isPrimary(),
                media.getSortOrder());
    }
}
