package com.loyalsuit.modules.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Slug is required")
    @Size(max = 255)
    private String slug;

    private String description;
    private String imageUrl;
    private UUID parentId;
    private int sortOrder = 0;
}
