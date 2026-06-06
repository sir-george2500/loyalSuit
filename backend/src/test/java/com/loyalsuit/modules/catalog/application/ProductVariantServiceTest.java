package com.loyalsuit.modules.catalog.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.dto.VariantRequest;
import com.loyalsuit.modules.catalog.application.dto.VariantResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductVariant;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class ProductVariantServiceTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductVariantService variantService;

    private UUID tenantId;
    private UUID productId;
    private UUID variantId;
    private Product product;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        product = new Product(tenantId, "Shoe", "shoe", BigDecimal.valueOf(50));
    }

    private VariantRequest request() {
        var r = new VariantRequest();
        r.setName("Size 42");
        r.setSku("SHOE-42");
        r.setPrice(BigDecimal.valueOf(55));
        return r;
    }

    private ProductVariant variant(UUID ofProductId) {
        var v = new ProductVariant(ofProductId, "Size 41", BigDecimal.valueOf(50));
        ReflectionTestUtils.setField(v, "id", variantId);
        return v;
    }

    // ---- tenant-safety boundary --------------------------------------------

    @Test
    void list_throwsNotFound_whenProductNotInTenant() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> variantService.list(productId, tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(variantRepository, never()).findByProductId(any());
    }

    @Test
    void create_throwsNotFound_whenProductNotInTenant() {
        // Arrange — caller tries to add a variant onto another tenant's product
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> variantService.create(productId, tenantId, request()))
                .isInstanceOf(NotFoundException.class);
        verify(variantRepository, never()).save(any());
    }

    @Test
    void update_throwsNotFound_whenVariantBelongsToAnotherProduct() {
        // Arrange — variant exists, but under a different product than the path
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(UUID.randomUUID())));

        // Act & Assert
        assertThatThrownBy(() -> variantService.update(productId, variantId, tenantId, request()))
                .isInstanceOf(NotFoundException.class);
        verify(variantRepository, never()).save(any());
    }

    @Test
    void delete_throwsNotFound_whenVariantMissing() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> variantService.delete(productId, variantId, tenantId))
                .isInstanceOf(NotFoundException.class);
        verify(variantRepository, never()).deleteById(any());
    }

    // ---- happy paths --------------------------------------------------------

    @Test
    void list_returnsVariants_whenProductIsOwned() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductId(productId)).thenReturn(List.of(variant(productId)));

        // Act
        List<VariantResponse> result = variantService.list(productId, tenantId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Size 41");
    }

    @Test
    void create_savesVariantUnderProduct() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        VariantResponse response = variantService.create(productId, tenantId, request());

        // Assert
        assertThat(response.name()).isEqualTo("Size 42");
        assertThat(response.sku()).isEqualTo("SHOE-42");
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.price()).isEqualByComparingTo("55");
    }

    @Test
    void update_modifiesVariant_whenItBelongsToProduct() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(productId)));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        VariantResponse response = variantService.update(productId, variantId, tenantId, request());

        // Assert
        assertThat(response.name()).isEqualTo("Size 42");
        assertThat(response.price()).isEqualByComparingTo("55");
    }

    @Test
    void delete_removesVariant_whenItBelongsToProduct() {
        // Arrange
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant(productId)));

        // Act
        variantService.delete(productId, variantId, tenantId);

        // Assert
        verify(variantRepository).deleteById(variantId);
    }
}
