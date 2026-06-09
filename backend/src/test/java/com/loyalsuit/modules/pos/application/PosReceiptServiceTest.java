package com.loyalsuit.modules.pos.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.pos.application.PosReceiptService.RenderedReceipt;
import com.loyalsuit.modules.pos.application.dto.ReceiptView;
import com.loyalsuit.modules.pos.domain.PosSale;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import com.loyalsuit.modules.pos.infrastructure.receipt.ReceiptPdfRenderer;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosReceiptServiceTest {

    @Mock private PosSaleRepository saleRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private ReceiptPdfRenderer renderer;

    @InjectMocks private PosReceiptService service;

    @Captor private ArgumentCaptor<ReceiptView> viewCaptor;

    private UUID tenantId;
    private UUID saleId;
    private UUID orderId;
    private UUID cashierId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        saleId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        cashierId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private PosSale sale() {
        PosSale sale = new PosSale(tenantId, orderId, "POS-ABC-123", cashierId, UUID.randomUUID(),
                "client-1", new BigDecimal("10.00"), new BigDecimal("10.00"),
                new BigDecimal("20.00"), new BigDecimal("10.00"), 2);
        ReflectionTestUtils.setField(sale, "id", saleId);
        return sale;
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant("Acme Store", "acme");
        tenant.setCurrency("USD");
        tenant.setTimezone("UTC");
        return tenant;
    }

    private OrderItem item(UUID product, int qty, String unit) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(product);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(unit));
        item.setTotal(new BigDecimal(unit).multiply(BigDecimal.valueOf(qty)));
        return item;
    }

    private Product product(UUID id, String name) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName(name);
        return product;
    }

    private void stubRender() {
        when(renderer.render(any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});
    }

    @Test
    void renderReceipt_buildsTheView_fromTheSaleOrderCatalogAndCashier() {
        // Arrange
        when(saleRepository.findByIdAndTenantId(saleId, tenantId)).thenReturn(Optional.of(sale()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item(productId, 2, "5.00")));
        when(productRepository.findByIdInAndTenantId(anyCollection(), eq(tenantId)))
                .thenReturn(List.of(product(productId, "Blue Widget")));
        when(userRepository.findById(cashierId)).thenReturn(Optional.of(
                new AppUser(tenantId, "jane@acme.dev", "hash", "Jane Doe", UserRole.STAFF)));
        stubRender();

        // Act
        RenderedReceipt receipt = service.renderReceipt(tenantId, saleId);

        // Assert — filename is built from the receipt number; the view carries the resolved data
        assertThat(receipt.fileName()).isEqualTo("receipt-POS-ABC-123.pdf");
        verify(renderer).render(viewCaptor.capture());
        ReceiptView view = viewCaptor.getValue();
        assertThat(view.storeName()).isEqualTo("Acme Store");
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.orderNumber()).isEqualTo("POS-ABC-123");
        assertThat(view.cashierName()).isEqualTo("Jane Doe");
        assertThat(view.lines()).singleElement()
                .satisfies(line -> {
                    assertThat(line.name()).isEqualTo("Blue Widget");
                    assertThat(line.quantity()).isEqualTo(2);
                    assertThat(line.lineTotal()).isEqualByComparingTo("10.00");
                });
        assertThat(view.total()).isEqualByComparingTo("10.00");
        assertThat(view.itemCount()).isEqualTo(2);
    }

    @Test
    void renderReceipt_aMissingSale_is404() {
        // Arrange
        when(saleRepository.findByIdAndTenantId(saleId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.renderReceipt(tenantId, saleId))
                .isInstanceOf(NotFoundException.class);
        verify(renderer, never()).render(any());
    }

    @Test
    void renderReceipt_aMissingTenant_is404() {
        // Arrange
        when(saleRepository.findByIdAndTenantId(saleId, tenantId)).thenReturn(Optional.of(sale()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.renderReceipt(tenantId, saleId))
                .isInstanceOf(NotFoundException.class);
        verify(renderer, never()).render(any());
    }

    @Test
    void renderReceipt_aDeletedProduct_fallsBackToAPlaceholderName() {
        // Arrange — the order line references a product no longer in the catalog
        when(saleRepository.findByIdAndTenantId(saleId, tenantId)).thenReturn(Optional.of(sale()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item(productId, 1, "5.00")));
        when(productRepository.findByIdInAndTenantId(anyCollection(), eq(tenantId))).thenReturn(List.of());
        when(userRepository.findById(cashierId)).thenReturn(Optional.empty());
        stubRender();

        // Act
        service.renderReceipt(tenantId, saleId);

        // Assert
        verify(renderer).render(viewCaptor.capture());
        assertThat(viewCaptor.getValue().lines()).singleElement()
                .satisfies(line -> assertThat(line.name()).isEqualTo("Item"));
    }

    @Test
    void renderReceipt_anUnknownCashier_leavesTheNameNull() {
        // Arrange
        when(saleRepository.findByIdAndTenantId(saleId, tenantId)).thenReturn(Optional.of(sale()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item(productId, 1, "5.00")));
        when(productRepository.findByIdInAndTenantId(anyCollection(), eq(tenantId)))
                .thenReturn(List.of(product(productId, "Blue Widget")));
        when(userRepository.findById(cashierId)).thenReturn(Optional.empty());
        stubRender();

        // Act
        service.renderReceipt(tenantId, saleId);

        // Assert
        verify(renderer).render(viewCaptor.capture());
        assertThat(viewCaptor.getValue().cashierName()).isNull();
    }

    @Test
    void renderReceipt_aCashierWithoutAFullName_usesTheirEmail() {
        // Arrange
        when(saleRepository.findByIdAndTenantId(saleId, tenantId)).thenReturn(Optional.of(sale()));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item(productId, 1, "5.00")));
        when(productRepository.findByIdInAndTenantId(anyCollection(), eq(tenantId)))
                .thenReturn(List.of(product(productId, "Blue Widget")));
        when(userRepository.findById(cashierId)).thenReturn(Optional.of(
                new AppUser(tenantId, "jane@acme.dev", "hash", null, UserRole.STAFF)));
        stubRender();

        // Act
        service.renderReceipt(tenantId, saleId);

        // Assert
        verify(renderer).render(viewCaptor.capture());
        assertThat(viewCaptor.getValue().cashierName()).isEqualTo("jane@acme.dev");
    }
}
