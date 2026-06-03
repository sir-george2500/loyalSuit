package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.CategoryResponse;
import com.loyalsuit.modules.catalog.application.dto.CreateCategoryRequest;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryResponse> listByTenant(UUID tenantId) {
        return categoryRepository.findAllByTenantId(tenantId)
                .stream()
                .map(CategoryResponse::new)
                .toList();
    }

    public CategoryResponse getById(UUID id, UUID tenantId) {
        return categoryRepository.findByIdAndTenantId(id, tenantId)
                .map(CategoryResponse::new)
                .orElseThrow(() -> new NotFoundException("Category", id));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, UUID tenantId) {
        if (categoryRepository.existsBySlugAndTenantId(request.getSlug(), tenantId)) {
            throw new ConflictException("Category slug already exists: " + request.getSlug());
        }

        Category category = new Category(tenantId, request.getName(), request.getSlug());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder());

        return new CategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CreateCategoryRequest request, UUID tenantId) {
        Category category = categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Category", id));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setSortOrder(request.getSortOrder());

        return new CategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id, UUID tenantId) {
        categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Category", id));
        categoryRepository.deleteById(id);
    }
}
