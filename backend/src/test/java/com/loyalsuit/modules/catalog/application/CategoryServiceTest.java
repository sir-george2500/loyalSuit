package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.CategoryResponse;
import com.loyalsuit.modules.catalog.application.dto.CreateCategoryRequest;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
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

    @Mock
    private ProductRepository productRepository;

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

    @Test
    void create_rejectsParentThatIsNotInThisTenant() {
        // Arrange — parentId resolves to nothing within the tenant (cross-tenant or bogus)
        UUID parentId = UUID.randomUUID();
        var request = new CreateCategoryRequest();
        request.setName("Laptops");
        request.setSlug("laptops");
        request.setParentId(parentId);
        when(categoryRepository.existsBySlugAndTenantId("laptops", tenantId)).thenReturn(false);
        when(categoryRepository.findByIdAndTenantId(parentId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.create(request, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Parent category does not exist");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_acceptsValidSameTenantParent() {
        // Arrange
        UUID parentId = UUID.randomUUID();
        var parent = new Category(tenantId, "Electronics", "electronics");
        var request = new CreateCategoryRequest();
        request.setName("Laptops");
        request.setSlug("laptops");
        request.setParentId(parentId);
        when(categoryRepository.existsBySlugAndTenantId("laptops", tenantId)).thenReturn(false);
        when(categoryRepository.findByIdAndTenantId(parentId, tenantId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse response = categoryService.create(request, tenantId);

        // Assert
        assertThat(response.getParentId()).isEqualTo(parentId);
    }

    @Test
    void update_rejectsCategoryAsItsOwnParent() {
        // Arrange
        var category = new Category(tenantId, "Electronics", "electronics");
        var request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setSlug("electronics");
        request.setParentId(categoryId); // its own id
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(categoryId, request, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("its own parent");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_rejectsParentThatWouldCreateACycle() {
        // Arrange — proposed parent's ancestor chain leads back to the category being edited
        UUID parentId = UUID.randomUUID();
        var category = new Category(tenantId, "Electronics", "electronics");
        var parent = new Category(tenantId, "Laptops", "laptops");
        parent.setParentId(categoryId); // parent is a child of the category → cycle
        var request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setSlug("electronics");
        request.setParentId(parentId);
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByIdAndTenantId(parentId, tenantId)).thenReturn(Optional.of(parent));

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(categoryId, request, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("circular");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_reparentsToValidParent() {
        // Arrange
        UUID parentId = UUID.randomUUID();
        var category = new Category(tenantId, "Electronics", "electronics");
        var parent = new Category(tenantId, "Root", "root"); // parentId null → no cycle
        var request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setSlug("electronics");
        request.setParentId(parentId);
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByIdAndTenantId(parentId, tenantId)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse response = categoryService.update(categoryId, request, tenantId);

        // Assert
        assertThat(response.getParentId()).isEqualTo(parentId);
    }

    @Test
    void update_changesSlug_whenNewSlugIsAvailable() {
        // Arrange
        var category = new Category(tenantId, "Electronics", "electronics");
        var request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setSlug("gadgets"); // changed
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugAndTenantId("gadgets", tenantId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse response = categoryService.update(categoryId, request, tenantId);

        // Assert
        assertThat(response.getSlug()).isEqualTo("gadgets");
    }

    @Test
    void update_throwsConflict_whenNewSlugIsTaken() {
        // Arrange
        var category = new Category(tenantId, "Electronics", "electronics");
        var request = new CreateCategoryRequest();
        request.setName("Electronics");
        request.setSlug("clothing"); // collides with a sibling
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlugAndTenantId("clothing", tenantId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(categoryId, request, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("slug already exists");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_isBlockedWhenCategoryHasSubcategories() {
        // Arrange
        var category = new Category(tenantId, "Electronics", "electronics");
        var child = new Category(tenantId, "Laptops", "laptops");
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentIdAndTenantId(categoryId, tenantId)).thenReturn(List.of(child));

        // Act & Assert
        assertThatThrownBy(() -> categoryService.delete(categoryId, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("subcategories");
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_isBlockedWhenCategoryHasProducts() {
        // Arrange
        var category = new Category(tenantId, "Electronics", "electronics");
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentIdAndTenantId(categoryId, tenantId)).thenReturn(List.of());
        when(productRepository.existsByCategoryIdAndTenantId(categoryId, tenantId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.delete(categoryId, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("products");
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void delete_succeedsForEmptyLeafCategory() {
        // Arrange
        var category = new Category(tenantId, "Electronics", "electronics");
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
        when(categoryRepository.findByParentIdAndTenantId(categoryId, tenantId)).thenReturn(List.of());
        when(productRepository.existsByCategoryIdAndTenantId(categoryId, tenantId)).thenReturn(false);

        // Act
        categoryService.delete(categoryId, tenantId);

        // Assert
        verify(categoryRepository).deleteById(categoryId);
    }
}
