package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.CreateProductRequest;
import com.loyalsuit.modules.catalog.application.dto.ProductResponse;
import com.loyalsuit.modules.catalog.application.dto.UpdateProductRequest;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private ProductService productService;

    private UUID tenantId;
    private UUID productId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    private CreateProductRequest createRequest() {
        var request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSlug("test-product");
        request.setPrice(BigDecimal.valueOf(29.99));
        return request;
    }

    // ---- create -------------------------------------------------------------

    @Test
    void create_savesAsDraft_withoutVendorForAdminActor() {
        // Arrange
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse response = productService.create(createRequest(), tenantId, actorId, "TENANT_ADMIN");

        // Assert
        assertThat(response.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(response.getVendorId()).isNull(); // admin products are house products
    }

    @Test
    void create_stampsVendorIdFromPrincipal_forVendorActor() {
        // Arrange
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse response = productService.create(createRequest(), tenantId, actorId, "VENDOR");

        // Assert — ownership comes from the authenticated principal, never the client
        assertThat(response.getVendorId()).isEqualTo(actorId);
    }

    @Test
    void create_throwsConflict_whenSlugAlreadyExists() {
        // Arrange
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.create(createRequest(), tenantId, actorId, "TENANT_ADMIN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("test-product");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_rejectsCategoryFromAnotherTenant() {
        // Arrange — a category id that doesn't resolve within this tenant
        UUID categoryId = UUID.randomUUID();
        var request = createRequest();
        request.setCategoryId(categoryId);
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.create(request, tenantId, actorId, "TENANT_ADMIN"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Category does not exist");
        verify(productRepository, never()).save(any());
    }

    // ---- update -------------------------------------------------------------

    @Test
    void update_appliesEditableFields() {
        // Arrange
        var product = new Product(tenantId, "Old", "old-slug", BigDecimal.valueOf(10));
        var request = new UpdateProductRequest();
        request.setName("New name");
        request.setSlug("new-slug");
        request.setPrice(BigDecimal.valueOf(42));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndTenantId("new-slug", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse response = productService.update(productId, request, tenantId);

        // Assert
        assertThat(response.getName()).isEqualTo("New name");
        assertThat(response.getSlug()).isEqualTo("new-slug");
        assertThat(response.getPrice()).isEqualByComparingTo("42");
    }

    @Test
    void update_throwsConflict_whenNewSlugTaken() {
        // Arrange
        var product = new Product(tenantId, "Old", "old-slug", BigDecimal.valueOf(10));
        var request = new UpdateProductRequest();
        request.setName("Old");
        request.setSlug("taken-slug");
        request.setPrice(BigDecimal.valueOf(10));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndTenantId("taken-slug", tenantId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.update(productId, request, tenantId))
                .isInstanceOf(ConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_rejectsCategoryFromAnotherTenant() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        var product = new Product(tenantId, "P", "p", BigDecimal.valueOf(10));
        var request = new UpdateProductRequest();
        request.setName("P");
        request.setSlug("p"); // unchanged slug → no uniqueness lookup
        request.setPrice(BigDecimal.valueOf(10));
        request.setCategoryId(categoryId);
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.update(productId, request, tenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Category does not exist");
        verify(productRepository, never()).save(any());
    }

    @Test
    void update_throwsNotFound_whenProductNotInTenant() {
        // Arrange
        var request = new UpdateProductRequest();
        request.setName("X");
        request.setSlug("x");
        request.setPrice(BigDecimal.valueOf(1));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.update(productId, request, tenantId))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- status lifecycle ---------------------------------------------------

    @Test
    void publish_then_unpublish_then_archive_transitionStatus() {
        // Arrange
        var product = new Product(tenantId, "Product", "product", BigDecimal.valueOf(50));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        // Act & Assert
        assertThat(productService.publish(productId, tenantId).getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(productService.unpublish(productId, tenantId).getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(productService.archive(productId, tenantId).getStatus()).isEqualTo(ProductStatus.ARCHIVED);
    }

    // ---- reads & delete -----------------------------------------------------

    @Test
    void getById_throwsNotFound_whenProductNotInTenant() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getById(productId, tenantId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listByTenant_returnsPaginatedResults() {
        // Arrange
        var product = new Product(tenantId, "P1", "p1", BigDecimal.valueOf(10));
        var pageable = PageRequest.of(0, 20);
        when(productRepository.findByTenantId(tenantId, pageable))
                .thenReturn(new PageImpl<>(List.of(product)));

        // Act
        var page = productService.listByTenant(tenantId, pageable);

        // Assert
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void delete_removesProduct_whenItExistsInTenant() {
        // Arrange
        var product = new Product(tenantId, "P", "p", BigDecimal.valueOf(10));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        // Act
        productService.delete(productId, tenantId);

        // Assert
        verify(productRepository).deleteById(productId);
    }

    @Test
    void delete_throwsNotFound_whenProductMissing() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.delete(productId, tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).deleteById(any());
    }
}
