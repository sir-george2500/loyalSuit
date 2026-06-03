package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.CategoryResponse;
import com.loyalsuit.modules.catalog.application.dto.CreateCategoryRequest;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID tenantId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    void create_savesAndReturnsCategory() {
        // Arrange
        var request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setSlug("electronics");

        var saved = new Category(tenantId, "Electronics", "electronics");

        when(categoryRepository.existsBySlugAndTenantId("electronics", tenantId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        // Act
        CategoryResponse response = categoryService.create(request, tenantId);

        // Assert
        assertThat(response.getName()).isEqualTo("Electronics");
        assertThat(response.getSlug()).isEqualTo("electronics");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void create_throwsConflict_whenSlugExists() {
        // Arrange
        var request = new CreateCategoryRequest();
        request.setName("Duplicate");
        request.setSlug("electronics");

        when(categoryRepository.existsBySlugAndTenantId("electronics", tenantId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.create(request, tenantId))
                .isInstanceOf(ConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenCategoryMissing() {
        // Arrange
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.getById(categoryId, tenantId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listByTenant_returnsAll() {
        // Arrange
        var cat1 = new Category(tenantId, "Electronics", "electronics");
        var cat2 = new Category(tenantId, "Clothing", "clothing");

        when(categoryRepository.findAllByTenantId(tenantId)).thenReturn(List.of(cat1, cat2));

        // Act
        List<CategoryResponse> result = categoryService.listByTenant(tenantId);

        // Assert
        assertThat(result).hasSize(2);
    }
}
