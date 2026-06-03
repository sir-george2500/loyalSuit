package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.CreateProductRequest;
import com.loyalsuit.modules.catalog.application.dto.ProductResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private UUID tenantId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void create_savesProductAndReturnsDraft() {
        // Arrange
        var request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSlug("test-product");
        request.setPrice(BigDecimal.valueOf(29.99));

        var savedProduct = new Product(tenantId, "Test Product", "test-product", BigDecimal.valueOf(29.99));

        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // Act
        ProductResponse response = productService.create(request, tenantId);

        // Assert
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getSlug()).isEqualTo("test-product");
        assertThat(response.getPrice()).isEqualByComparingTo("29.99");
        assertThat(response.getStatus()).isEqualTo(ProductStatus.DRAFT);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_throwsConflict_whenSlugAlreadyExists() {
        // Arrange
        var request = new CreateProductRequest();
        request.setName("Duplicate");
        request.setSlug("existing-slug");
        request.setPrice(BigDecimal.valueOf(10));

        when(productRepository.existsBySlugAndTenantId("existing-slug", tenantId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.create(request, tenantId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existing-slug");
        verify(productRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenProductNotInTenant() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getById(productId, tenantId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void publish_activatesProduct() {
        // Arrange
        var product = new Product(tenantId, "Product", "product", BigDecimal.valueOf(50));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);

        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        // Act
        ProductResponse response = productService.publish(productId, tenantId);

        // Assert
        assertThat(response.getStatus()).isEqualTo(ProductStatus.ACTIVE);
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
}
