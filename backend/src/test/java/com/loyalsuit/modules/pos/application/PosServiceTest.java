package com.loyalsuit.modules.pos.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.marketplace.application.CommissionLine;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.pos.application.dto.CompleteSaleRequest;
import com.loyalsuit.modules.pos.application.dto.PosProductResponse;
import com.loyalsuit.modules.pos.application.dto.PosSaleResponse;
import com.loyalsuit.modules.pos.application.dto.SaleLineRequest;
import com.loyalsuit.modules.pos.domain.PosSale;
import com.loyalsuit.modules.pos.domain.PosShift;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import com.loyalsuit.modules.pos.domain.port.PosShiftRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
class PosServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private StockService stockService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private com.loyalsuit.modules.marketplace.application.CommissionService commissionService;
    @Mock private PosSaleRepository posSaleRepository;
    @Mock private PosShiftRepository shiftRepository;

    @InjectMocks private PosService service;

    private UUID tenantId;
    private UUID cashierId;
    private UUID productId;
    private UUID vendorId;
    private UUID shiftId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        cashierId = UUID.randomUUID();
        productId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        shiftId = UUID.randomUUID();
    }

    /** Stub the cashier as having an open drawer (required to ring up a sale). */
    private void stubOpenShift() {
        PosShift shift = new PosShift(tenantId, cashierId, new BigDecimal("0.00"));
        ReflectionTestUtils.setField(shift, "id", shiftId);
        when(shiftRepository.findOpenByCashier(tenantId, cashierId)).thenReturn(Optional.of(shift));
    }

    private Tenant tenant() {
        Tenant t = new Tenant();
        t.setCurrency("USD");
        return t;
    }

    private Product product(String price, UUID vendor) {
        Product p = new Product(tenantId, "Coffee", "coffee", new BigDecimal(price));
        p.setStatus(ProductStatus.ACTIVE);
        p.setVendorId(vendor);
        ReflectionTestUtils.setField(p, "id", productId);
        return p;
    }

    private CompleteSaleRequest saleOf(int quantity, String tendered) {
        SaleLineRequest line = new SaleLineRequest();
        line.setProductId(productId);
        line.setQuantity(quantity);
        CompleteSaleRequest request = new CompleteSaleRequest();
        request.setClientSaleId("sale-" + UUID.randomUUID());
        request.setItems(List.of(line));
        request.setAmountTendered(new BigDecimal(tendered));
        return request;
    }

    private void stubOrderSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });
    }

    private void stubPosSaleSave() {
        when(posSaleRepository.save(any(PosSale.class))).thenAnswer(inv -> {
            PosSale s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
            return s;
        });
    }

    // ---- completeSale -------------------------------------------------------

    @Test
    void completeSale_pricesFromCatalog_returnsChange_andDecrementsStock() {
        // Arrange — 2 × $10 = $20, customer pays $50 → $30 change
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        stubOpenShift();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product("10.00", vendorId)));
        stubOrderSave();
        stubPosSaleSave();

        // Act
        PosSaleResponse sale = service.completeSale(tenantId, cashierId, saleOf(2, "50.00"));

        // Assert — server-priced totals, correct change, stock decremented
        assertThat(sale.subtotal()).isEqualByComparingTo("20.00");
        assertThat(sale.total()).isEqualByComparingTo("20.00");
        assertThat(sale.changeGiven()).isEqualByComparingTo("30.00");
        assertThat(sale.itemCount()).isEqualTo(2);
        assertThat(sale.cashierId()).isEqualTo(cashierId);
        assertThat(sale.orderNumber()).startsWith("POS-");
        verify(stockService).reserve(tenantId, productId, null, 2);
    }

    @Test
    void completeSale_createsPaidDeliveredOrder() {
        // Arrange
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        stubOpenShift();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product("10.00", vendorId)));
        stubOrderSave();
        stubPosSaleSave();

        // Act
        service.completeSale(tenantId, cashierId, saleOf(1, "10.00"));

        // Assert — the in-store sale lands as a paid, delivered order (the shared ledger)
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getValue().getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getValue().getTotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void completeSale_settlesCommissionForVendorLines() {
        // Arrange — a vendor product, so a commission line is settled when cash is in
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        stubOpenShift();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product("10.00", vendorId)));
        stubOrderSave();
        stubPosSaleSave();

        // Act
        service.completeSale(tenantId, cashierId, saleOf(3, "30.00"));

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CommissionLine>> lines = ArgumentCaptor.forClass(List.class);
        verify(commissionService).settleOrder(eq(tenantId), any(), any(), lines.capture());
        assertThat(lines.getValue()).hasSize(1);
        assertThat(lines.getValue().get(0).vendorId()).isEqualTo(vendorId);
        assertThat(lines.getValue().get(0).grossAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void completeSale_houseProduct_settlesNoCommission() {
        // Arrange — house product (no vendor) → no commission lines
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        stubOpenShift();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product("10.00", null)));
        stubOrderSave();
        stubPosSaleSave();

        // Act
        service.completeSale(tenantId, cashierId, saleOf(1, "10.00"));

        // Assert
        verify(commissionService).settleOrder(eq(tenantId), any(), any(), eq(List.of()));
    }

    @Test
    void completeSale_insufficientTender_isRejected_andNoOrderCreated() {
        // Arrange — $10 due, only $5 tendered
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        stubOpenShift();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product("10.00", vendorId)));

        // Act & Assert
        assertThatThrownBy(() -> service.completeSale(tenantId, cashierId, saleOf(1, "5.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("less than the total");
        verify(orderRepository, never()).save(any());
        verify(posSaleRepository, never()).save(any());
    }

    @Test
    void completeSale_unavailableProduct_isRejected() {
        // Arrange — the product can't be found (or isn't active)
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        stubOpenShift();
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.completeSale(tenantId, cashierId, saleOf(1, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void completeSale_withoutAnOpenShift_isRejected() {
        // Arrange — no open drawer for this cashier
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        when(posSaleRepository.findByTenantIdAndClientSaleId(eq(tenantId), any())).thenReturn(Optional.empty());
        when(shiftRepository.findOpenByCashier(tenantId, cashierId)).thenReturn(Optional.empty());

        // Act & Assert — a cash sale can't be rung up without a drawer to reconcile
        assertThatThrownBy(() -> service.completeSale(tenantId, cashierId, saleOf(1, "10.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Open a shift");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void completeSale_isIdempotent_onClientSaleId() {
        // Arrange — a sale already exists for this client sale id
        PosSale existing = new PosSale(tenantId, UUID.randomUUID(), "POS-ABC-123", cashierId, shiftId, "sale-1",
                new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("50.00"),
                new BigDecimal("30.00"), 2);
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant()));
        CompleteSaleRequest request = saleOf(2, "50.00");
        request.setClientSaleId("sale-1");
        when(posSaleRepository.findByTenantIdAndClientSaleId(tenantId, "sale-1")).thenReturn(Optional.of(existing));

        // Act — re-submitting returns the original sale, ringing nothing up again
        PosSaleResponse sale = service.completeSale(tenantId, cashierId, request);

        // Assert
        assertThat(sale.orderNumber()).isEqualTo("POS-ABC-123");
        verify(orderRepository, never()).save(any());
        verify(stockService, never()).reserve(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
        verify(posSaleRepository, never()).save(any());
    }

    // ---- searchProducts -----------------------------------------------------

    @Test
    void searchProducts_withQuery_searchesActiveCatalog() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 24);
        when(productRepository.searchActive(tenantId, "cof", pageable))
                .thenReturn(new PageImpl<>(List.of(product("10.00", vendorId))));

        // Act
        PageResponse<PosProductResponse> page = service.searchProducts(tenantId, "cof", pageable);

        // Assert
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).name()).isEqualTo("Coffee");
    }

    @Test
    void searchProducts_blankQuery_listsAllActive() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 24);
        when(productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(product("10.00", null))));

        // Act
        PageResponse<PosProductResponse> page = service.searchProducts(tenantId, "  ", pageable);

        // Assert
        assertThat(page.getContent()).hasSize(1);
        verify(productRepository, never()).searchActive(any(), any(), any());
    }
}
