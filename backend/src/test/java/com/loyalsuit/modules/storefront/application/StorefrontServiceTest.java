package com.loyalsuit.modules.storefront.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.domain.Category;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductMedia;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.CategoryRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import com.loyalsuit.modules.inventory.domain.port.StockRepository;
import com.loyalsuit.modules.storefront.application.dto.StoreProductDetail;
import com.loyalsuit.modules.storefront.application.dto.StoreProductSummary;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorefrontServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductMediaRepository mediaRepository;
    @Mock private StockRepository stockRepository;
    @Mock private com.loyalsuit.modules.marketplace.domain.port.VendorRepository vendorRepository;

    @InjectMocks private StorefrontService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private Tenant store(boolean active) {
        Tenant t = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(t, "id", tenantId);
        t.setActive(active);
        return t;
    }

    private Product product(String slug, ProductStatus status, UUID id) {
        Product p = new Product(tenantId, "Widget", slug, BigDecimal.valueOf(20));
        p.setStatus(status);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    // ---- store resolution (the public access boundary) ----------------------

    @Test
    void getStore_returnsActiveStore() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));

        // Act & Assert
        assertThat(service.getStore("acme").name()).isEqualTo("Acme");
    }

    @Test
    void getStore_hidesInactiveStore() {
        // Arrange — a suspended store must not be browsable
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(false)));

        // Act & Assert
        assertThatThrownBy(() -> service.getStore("acme")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getStore_unknownSlug_is404() {
        // Arrange
        when(tenantRepository.findBySlug("ghost")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getStore("ghost")).isInstanceOf(NotFoundException.class);
    }

    // ---- marketplace index: only stocked, active stores ---------------------

    @Test
    void listStores_returnsStockedStores_orderedByProductCount() {
        // Arrange — two active stores with products, plus an empty one
        Tenant big = store(true);
        Tenant small = new Tenant("Bravo", "bravo");
        UUID smallId = UUID.randomUUID();
        ReflectionTestUtils.setField(small, "id", smallId);
        Tenant empty = new Tenant("Ghost", "ghost");
        UUID emptyId = UUID.randomUUID();
        ReflectionTestUtils.setField(empty, "id", emptyId);

        when(tenantRepository.findAllActive()).thenReturn(List.of(small, big, empty));
        when(productRepository.countActiveProductsByTenant(any()))
                .thenReturn(java.util.Map.of(tenantId, 5L, smallId, 2L)); // empty store absent → 0

        // Act
        var result = service.listStores();

        // Assert — empty store hidden, busiest store first
        assertThat(result).extracting(s -> s.slug()).containsExactly("acme", "bravo");
        assertThat(result.get(0).productCount()).isEqualTo(5L);
    }

    @Test
    void listStores_isEmptyWhenNoActiveStores() {
        // Arrange
        when(tenantRepository.findAllActive()).thenReturn(List.of());

        // Act & Assert
        assertThat(service.listStores()).isEmpty();
    }

    // ---- products: only ACTIVE, with batch lookups --------------------------

    @Test
    void products_listsActiveProducts_withCategoryNameAndPrimaryImage() {
        // Arrange
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product p = product("widget", ProductStatus.ACTIVE, productId);
        p.setCategoryId(categoryId);
        Category cat = new Category(tenantId, "Gadgets", "gadgets");
        ReflectionTestUtils.setField(cat, "id", categoryId);
        ProductMedia media = new ProductMedia(tenantId, productId, "pid", "https://cdn/img.png");

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(productRepository.findByTenantIdAndStatus(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p)));
        when(categoryRepository.findAllByTenantId(tenantId)).thenReturn(List.of(cat));
        when(mediaRepository.findByProductIdInAndPrimaryTrue(List.of(productId))).thenReturn(List.of(media));

        // Act
        List<StoreProductSummary> result = service.products("acme", null, PageRequest.of(0, 24)).getContent();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryName()).isEqualTo("Gadgets");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://cdn/img.png");
    }

    // ---- product detail: never expose unpublished ---------------------------

    @Test
    void product_returnsActiveDetail_withInStockFlag() {
        // Arrange
        UUID productId = UUID.randomUUID();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(productRepository.findBySlugAndTenantId("widget", tenantId))
                .thenReturn(Optional.of(product("widget", ProductStatus.ACTIVE, productId)));
        when(mediaRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(List.of());
        when(variantRepository.findByProductId(productId)).thenReturn(List.of());
        when(stockRepository.hasStock(productId, tenantId)).thenReturn(true);

        // Act
        StoreProductDetail detail = service.product("acme", "widget");

        // Assert
        assertThat(detail.slug()).isEqualTo("widget");
        assertThat(detail.inStock()).isTrue();
    }

    @Test
    void product_hidesUnpublishedProduct() {
        // Arrange — a DRAFT product must look non-existent to the public
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(productRepository.findBySlugAndTenantId("secret", tenantId))
                .thenReturn(Optional.of(product("secret", ProductStatus.DRAFT, UUID.randomUUID())));

        // Act & Assert
        assertThatThrownBy(() -> service.product("acme", "secret")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void product_unknownSlug_is404() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(productRepository.findBySlugAndTenantId("nope", tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.product("acme", "nope")).isInstanceOf(NotFoundException.class);
    }

    // ---- categories: only active --------------------------------------------

    @Test
    void categories_returnsOnlyActiveOnes() {
        // Arrange
        Category active = new Category(tenantId, "Active", "active");
        Category inactive = new Category(tenantId, "Hidden", "hidden");
        inactive.setActive(false);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(categoryRepository.findAllByTenantId(tenantId)).thenReturn(List.of(active, inactive));

        // Act
        var result = service.categories("acme");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).slug()).isEqualTo("active");
    }
}
