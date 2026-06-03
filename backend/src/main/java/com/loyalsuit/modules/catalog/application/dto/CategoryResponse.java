package com.loyalsuit.modules.catalog.application.dto;

import com.loyalsuit.modules.catalog.domain.Category;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class CategoryResponse {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String description;
    private final String imageUrl;
    private final UUID parentId;
    private final boolean active;
    private final int sortOrder;
    private final Instant createdAt;

    public CategoryResponse(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.slug = category.getSlug();
        this.description = category.getDescription();
        this.imageUrl = category.getImageUrl();
        this.parentId = category.getParentId();
        this.active = category.isActive();
        this.sortOrder = category.getSortOrder();
        this.createdAt = category.getCreatedAt();
    }
}
