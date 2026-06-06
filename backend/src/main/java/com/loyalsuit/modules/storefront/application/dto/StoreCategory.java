package com.loyalsuit.modules.storefront.application.dto;

import com.loyalsuit.modules.catalog.domain.Category;

public record StoreCategory(String name, String slug) {

    public static StoreCategory from(Category category) {
        return new StoreCategory(category.getName(), category.getSlug());
    }
}
