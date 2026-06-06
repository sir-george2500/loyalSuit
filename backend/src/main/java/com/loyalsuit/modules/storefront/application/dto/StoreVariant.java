package com.loyalsuit.modules.storefront.application.dto;

import com.loyalsuit.modules.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreVariant(UUID id, String name, BigDecimal price) {

    public static StoreVariant from(ProductVariant variant) {
        return new StoreVariant(variant.getId(), variant.getName(), variant.getPrice());
    }
}
