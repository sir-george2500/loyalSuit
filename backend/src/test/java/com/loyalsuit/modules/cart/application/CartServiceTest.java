package com.loyalsuit.modules.cart.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.cart.application.dto.AddItemRequest;
import com.loyalsuit.modules.cart.application.dto.CartView;
import com.loyalsuit.modules.cart.domain.Cart;
import com.loyalsuit.modules.cart.domain.port.CartRepository;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.ProductVariant;
import com.loyalsuit.modules.catalog.domain.port.ProductMediaRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.catalog.domain.port.ProductVariantRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
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
class CartServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductMediaRepository mediaRepository;
    @Mock private CartRepository cartRepository;

    @InjectMocks private CartService cartService;

    private UUID tenantId;
    private UUID productId;
    private static final String TOKEN = "cart-token-123";

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Tenant store() {
        Tenant t = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(t, "id", tenantId);
        t.setActive(true);
        t.setCurrency("USD");
        return t;
    }

    private Product product(ProductStatus status, BigDecimal price) {
        Product p = new Product(tenantId, "Widget", "widget", price);
        p.setStatus(status);
        ReflectionTestUtils.setField(p, "id", productId);
        return p;
    }

    private AddItemRequest add(UUID variantId, int qty) {
        var r = new AddItemRequest();
        r.setProductId(productId);
        r.setVariantId(variantId);
        r.setQuantity(qty);
        return r;
    }

    // ---- add ----------------------------------------------------------------

    @Test
    void addItem_addsActiveProduct_andComputesLineTotal() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(ProductStatus.ACTIVE, BigDecimal.valueOf(20))));
        when(cartRepository.find(tenantId, TOKEN)).thenReturn(Optional.empty());
        when(mediaRepository.findByProductIdInAndPrimaryTrue(any())).thenReturn(List.of());

        // Act
        CartView view = cartService.addItem("acme", TOKEN, add(null, 2));

        // Assert
        assertThat(view.itemCount()).isEqualTo(2);
        assertThat(view.subtotal()).isEqualByComparingTo("40");
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).unitPrice()).isEqualByComparingTo("20");
        verify(cartRepository).save(any(), any(), any());
    }

    @Test
    void addItem_rejectsInactiveProduct() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(ProductStatus.DRAFT, BigDecimal.TEN)));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItem("acme", TOKEN, add(null, 1)))
                .isInstanceOf(BusinessException.class);
        verify(cartRepository, never()).save(any(), any(), any());
    }

    @Test
    void addItem_rejectsUnknownProduct() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItem("acme", TOKEN, add(null, 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addItem_rejectsVariantNotBelongingToProduct() {
        // Arrange — variant exists but under a different product
        UUID variantId = UUID.randomUUID();
        var variant = new ProductVariant(UUID.randomUUID(), "S", BigDecimal.TEN);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(ProductStatus.ACTIVE, BigDecimal.TEN)));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        // Act & Assert
        assertThatThrownBy(() -> cartService.addItem("acme", TOKEN, add(variantId, 1)))
                .isInstanceOf(BusinessException.class);
        verify(cartRepository, never()).save(any(), any(), any());
    }

    // ---- view: server-side recomputation ------------------------------------

    @Test
    void view_recomputesPriceFromCurrentCatalog_notFromTheCart() {
        // Arrange — cart holds a line; the product's CURRENT price is 99
        Cart cart = new Cart(tenantId);
        cart.addOrIncrement(productId, null, 3);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(cartRepository.find(tenantId, TOKEN)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(ProductStatus.ACTIVE, BigDecimal.valueOf(99))));
        when(mediaRepository.findByProductIdInAndPrimaryTrue(any())).thenReturn(List.of());

        // Act
        CartView view = cartService.view("acme", TOKEN);

        // Assert — price reflects the live catalog, line total = 99 * 3
        assertThat(view.items().get(0).unitPrice()).isEqualByComparingTo("99");
        assertThat(view.subtotal()).isEqualByComparingTo("297");
    }

    @Test
    void view_usesVariantPriceWhenAVariantIsChosen() {
        // Arrange
        UUID variantId = UUID.randomUUID();
        Cart cart = new Cart(tenantId);
        cart.addOrIncrement(productId, variantId, 1);
        var variant = new ProductVariant(productId, "Large", BigDecimal.valueOf(35));
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(cartRepository.find(tenantId, TOKEN)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndTenantId(productId, tenantId))
                .thenReturn(Optional.of(product(ProductStatus.ACTIVE, BigDecimal.valueOf(20))));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(mediaRepository.findByProductIdInAndPrimaryTrue(any())).thenReturn(List.of());

        // Act
        CartView view = cartService.view("acme", TOKEN);

        // Assert — variant price wins over the base product price
        assertThat(view.items().get(0).unitPrice()).isEqualByComparingTo("35");
        assertThat(view.items().get(0).variantName()).isEqualTo("Large");
    }

    @Test
    void view_dropsLinesWhoseProductIsNoLongerAvailable() {
        // Arrange — the cart references a product that is now gone/unpublished
        Cart cart = new Cart(tenantId);
        cart.addOrIncrement(productId, null, 2);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(cartRepository.find(tenantId, TOKEN)).thenReturn(Optional.of(cart));
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());
        when(mediaRepository.findByProductIdInAndPrimaryTrue(any())).thenReturn(List.of());

        // Act
        CartView view = cartService.view("acme", TOKEN);

        // Assert — stale line dropped and the cleaned cart persisted
        assertThat(view.items()).isEmpty();
        assertThat(view.subtotal()).isEqualByComparingTo("0");
        verify(cartRepository).save(any(), any(), any());
    }

    @Test
    void view_hiddenStore_is404() {
        // Arrange
        Tenant suspended = store();
        suspended.setActive(false);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(suspended));

        // Act & Assert
        assertThatThrownBy(() -> cartService.view("acme", TOKEN))
                .isInstanceOf(com.loyalsuit.common.exception.NotFoundException.class);
    }
}
