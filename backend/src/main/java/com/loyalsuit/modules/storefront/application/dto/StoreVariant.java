package com.loyalsuit.modules.storefront.application.dto;

import com.loyalsuit.modules.catalog.domain.ProductVariant;

import java.math.BigDecimal;

public record StoreVariant(String name, BigDecimal price) {

    public static StoreVariant from(ProductVariant variant) {
        return new StoreVariant(variant.getName(), variant.getPrice());
    }
}
