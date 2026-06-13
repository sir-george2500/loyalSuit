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
import com.loyalsuit.modules.marketplace.application.VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String ADMIN = "TENANT_ADMIN";
    private static final String VENDOR = "VENDOR";

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private VendorService vendorService;

    @InjectMocks private ProductService productService;

    private UUID tenantId;
    private UUID productId;
    private UUID actorId;
    private UUID vendorPk; // the Vendor entity's id (≠ the user/actor id)

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        vendorPk = UUID.randomUUID();
    }

    private CreateProductRequest createRequest() {
        var request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSlug("test-product");
        request.setPrice(BigDecimal.valueOf(29.99));
        return request;
    }

    private Product product(UUID vendorId) {
        Product p = new Product(tenantId, "Test", "test", BigDecimal.TEN);
        ReflectionTestUtils.setField(p, "id", productId);
        p.setVendorId(vendorId);
        return p;
    }

    // ---- create -------------------------------------------------------------

    @Test
    void create_adminProduct_hasNoVendor() {
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(createRequest(), tenantId, actorId, ADMIN);

        assertThat(response.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(response.getVendorId()).isNull();
    }

    @Test
    void create_activeVendor_stampsTheirVendorId() {
        when(vendorService.requireActiveVendorId(actorId)).thenReturn(vendorPk);
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(createRequest(), tenantId, actorId, VENDOR);

        // Stamped with the Vendor PK (so attribution/storefronts resolve), not the user id.
        assertThat(response.getVendorId()).isEqualTo(vendorPk);
    }

    @Test
    void create_rejectsInactiveVendor() {
        when(vendorService.requireActiveVendorId(actorId))
                .thenThrow(new BusinessException("Your vendor account is not active"));

        assertThatThrownBy(() -> productService.create(createRequest(), tenantId, actorId, VENDOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_throwsConflict_whenSlugExists() {
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(true);

        assertThatThrownBy(() -> productService.create(createRequest(), tenantId, actorId, ADMIN))
                .isInstanceOf(ConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_rejectsCategoryFromAnotherTenant() {
        UUID categoryId = UUID.randomUUID();
        var request = createRequest();
        request.setCategoryId(categoryId);
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request, tenantId, actorId, ADMIN))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Category does not exist");
    }

    // ---- flash deals --------------------------------------------------------

    @Test
    void create_withAValidFlashDeal_schedulesTheSalePrice() {
        // Arrange — list 29.99, sale 19.99 running now
        var request = createRequest();
        request.setSalePrice(BigDecimal.valueOf(19.99));
        request.setSaleStartsAt(java.time.Instant.now().minusSeconds(60));
        request.setSaleEndsAt(java.time.Instant.now().plusSeconds(60));
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse response = productService.create(request, tenantId, actorId, ADMIN);

        // Assert
        assertThat(response.getSalePrice()).isEqualByComparingTo("19.99");
        assertThat(response.isOnSale()).isTrue();
    }

    @Test
    void create_aSalePriceAtOrAboveTheListPrice_isRejected() {
        // Arrange — sale 35 >= list 29.99
        var request = createRequest();
        request.setSalePrice(BigDecimal.valueOf(35.00));
        when(productRepository.existsBySlugAndTenantId("test-product", tenantId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> productService.create(request, tenantId, actorId, ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("below the regular price");
        verify(productRepository, never()).save(any());
    }

    // ---- vendor scoping -----------------------------------------------------

    @Test
    void listForActor_vendorSeesOnlyTheirOwnProducts() {
        when(vendorService.requireVendorId(actorId)).thenReturn(vendorPk);
        when(productRepository.findByTenantIdAndVendorId(tenantId, vendorPk, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(product(vendorPk))));

        var page = productService.listForActor(tenantId, actorId, VENDOR, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        verify(productRepository).findByTenantIdAndVendorId(tenantId, vendorPk, PageRequest.of(0, 20));
        verify(productRepository, never()).findByTenantId(any(), any());
    }

    @Test
    void listForActor_adminSeesTheWholeCatalog() {
        when(productRepository.findByTenantId(tenantId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(product(null), product(UUID.randomUUID()))));

        var page = productService.listForActor(tenantId, actorId, ADMIN, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        verify(productRepository, never()).findByTenantIdAndVendorId(any(), any(), any());
    }

    @Test
    void getById_vendor404sOnAnotherVendorsProduct() {
        when(vendorService.requireVendorId(actorId)).thenReturn(vendorPk);
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(UUID.randomUUID()))); // owned by someone else

        assertThatThrownBy(() -> productService.getById(productId, tenantId, actorId, VENDOR))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_vendorCanReadTheirOwnProduct() {
        when(vendorService.requireVendorId(actorId)).thenReturn(vendorPk);
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product(vendorPk)));

        assertThat(productService.getById(productId, tenantId, actorId, VENDOR)).isNotNull();
    }

    @Test
    void update_vendor404sOnAnotherVendorsProduct() {
        when(vendorService.requireActiveVendorId(actorId)).thenReturn(vendorPk);
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(UUID.randomUUID())));
        var request = new UpdateProductRequest();
        request.setName("X");
        request.setSlug("x");
        request.setPrice(BigDecimal.ONE);

        assertThatThrownBy(() -> productService.update(productId, request, tenantId, actorId, VENDOR))
                .isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).save(any());
    }

    // ---- update / lifecycle (admin) -----------------------------------------

    @Test
    void update_appliesEditableFields() {
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product(null)));
        when(productRepository.existsBySlugAndTenantId("new-slug", tenantId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        var request = new UpdateProductRequest();
        request.setName("New name");
        request.setSlug("new-slug");
        request.setPrice(BigDecimal.valueOf(42));

        ProductResponse response = productService.update(productId, request, tenantId, actorId, ADMIN);

        assertThat(response.getName()).isEqualTo("New name");
        assertThat(response.getPrice()).isEqualByComparingTo("42");
    }

    @Test
    void update_throwsNotFound_whenProductNotInTenant() {
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());
        var request = new UpdateProductRequest();
        request.setName("X");
        request.setSlug("x");
        request.setPrice(BigDecimal.ONE);

        assertThatThrownBy(() -> productService.update(productId, request, tenantId, actorId, ADMIN))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void statusTransitions_advanceProductState() {
        Product product = product(null);
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        assertThat(productService.publish(productId, tenantId, actorId, ADMIN).getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(productService.unpublish(productId, tenantId, actorId, ADMIN).getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(productService.archive(productId, tenantId, actorId, ADMIN).getStatus()).isEqualTo(ProductStatus.ARCHIVED);
    }

    // ---- delete (admin-only, tenant-scoped) ---------------------------------

    @Test
    void delete_removesProduct_whenItExistsInTenant() {
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product(null)));

        productService.delete(productId, tenantId);

        verify(productRepository).deleteById(productId);
    }

    @Test
    void delete_throwsNotFound_whenProductMissing() {
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(productId, tenantId)).isInstanceOf(NotFoundException.class);
        verify(productRepository, never()).deleteById(eq(productId));
    }
}
