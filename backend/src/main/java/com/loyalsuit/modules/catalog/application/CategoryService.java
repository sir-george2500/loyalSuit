package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.CategoryResponse;
import com.loyalsuit.modules.catalog.application.dto.CreateCategoryRequest;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

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
        validateParent(tenantId, null, request.getParentId());

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

        validateParent(tenantId, id, request.getParentId());

        // Slug is editable, but must stay unique per tenant. Only re-check when it
        // actually changes, so the DB unique constraint never surfaces as a 500.
        if (!category.getSlug().equals(request.getSlug())
                && categoryRepository.existsBySlugAndTenantId(request.getSlug(), tenantId)) {
            throw new ConflictException("Category slug already exists: " + request.getSlug());
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder());

        return new CategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id, UUID tenantId) {
        categoryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Category", id));

        // Refuse to silently orphan a subtree or detach products (the schema's
        // ON DELETE SET NULL would do exactly that). Force the caller to reassign.
        if (!categoryRepository.findByParentIdAndTenantId(id, tenantId).isEmpty()) {
            throw new ConflictException("Move or remove the subcategories before deleting this category");
        }
        if (productRepository.existsByCategoryIdAndTenantId(id, tenantId)) {
            throw new ConflictException("Reassign the products in this category before deleting it");
        }

        categoryRepository.deleteById(id);
    }

    /**
     * Validates a proposed parent for a category. The parent must exist within the
     * same tenant (so a category can never reference another tenant's tree), a
     * category cannot be its own parent, and re-parenting may not introduce a cycle
     * (i.e. the new parent must not be a descendant of the category being edited).
     *
     * @param categoryId the category being edited, or null when creating
     */
    private void validateParent(UUID tenantId, UUID categoryId, UUID parentId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(categoryId)) {
            throw new BusinessException("A category cannot be its own parent");
        }
        Category parent = categoryRepository.findByIdAndTenantId(parentId, tenantId)
                .orElseThrow(() -> new BusinessException("Parent category does not exist in this store"));

        // On create there is no existing node, so no cycle is possible — skip the walk.
        if (categoryId == null) {
            return;
        }

        // Re-parenting: walk up from the proposed parent. Reaching categoryId means
        // the new parent is a descendant (a cycle). The visited set guarantees
        // termination even if the stored data were already corrupt.
        Set<UUID> visited = new HashSet<>();
        UUID cursor = parent.getParentId();
        while (cursor != null && visited.add(cursor)) {
            if (cursor.equals(categoryId)) {
                throw new BusinessException("That parent would create a circular category tree");
            }
            cursor = categoryRepository.findByIdAndTenantId(cursor, tenantId)
                    .map(Category::getParentId)
                    .orElse(null);
        }
    }
}
