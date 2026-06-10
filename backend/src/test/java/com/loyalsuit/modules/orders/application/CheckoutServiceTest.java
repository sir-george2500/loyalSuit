package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.cart.application.CartService;
import com.loyalsuit.modules.cart.application.dto.CartItemView;
import com.loyalsuit.modules.cart.application.dto.CartView;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.orders.application.dto.CheckoutRequest;
import com.loyalsuit.modules.orders.application.port.ResolvedShippingZone;
import com.loyalsuit.modules.orders.application.port.ShippingZoneResolver;
import com.loyalsuit.modules.promotions.application.CouponService;
import com.loyalsuit.modules.promotions.application.dto.AppliedCoupon;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.PaymentMethod;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private CartService cartService;
    @Mock private StockService stockService;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ShippingZoneResolver shippingZoneResolver;
    @Mock private CouponService couponService;

    @InjectMocks private CheckoutService checkoutService;

    private UUID tenantId;
    private UUID productId;
    private static final String TOKEN = "tok";

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Tenant store(boolean active) {
        Tenant t = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(t, "id", tenantId);
        t.setActive(active);
        t.setCurrency("USD");
        return t;
    }

    private CartView cartWithOneLine() {
        var line = new CartItemView(productId, null, "Widget", "widget", null, null,
                BigDecimal.valueOf(20), 2, BigDecimal.valueOf(40));
        return new CartView(List.of(line), BigDecimal.valueOf(40), 2, "USD");
    }

    private CheckoutRequest request() {
        var r = new CheckoutRequest();
        r.setCustomerName("Jane Buyer");
        r.setAddressLine1("1 Market St");
        r.setCity("Springfield");
        return r;
    }

    // ---- happy path ---------------------------------------------------------

    @Test
    void checkout_createsCashOrder_reservesStock_andClearsCart() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });

        // Act — guest checkout (no authenticated user)
        OrderResponse response = checkoutService.checkout("acme", TOKEN, request(), null, null, null);

        // Assert
        assertThat(response.orderNumber()).startsWith("ORD-");
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CASH.name());
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.UNPAID.name());
        assertThat(response.total()).isEqualByComparingTo("40");
        assertThat(response.items()).hasSize(1);
        verify(stockService).reserve(tenantId, productId, null, 2);
        verify(orderItemRepository).saveAll(any());
        verify(cartService).clear("acme", TOKEN);
    }

    @Test
    void checkout_linksAuthenticatedCustomerOfTheStore() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act — authenticated customer of this tenant
        checkoutService.checkout("acme", TOKEN, request(), null, userId, tenantId);

        // Assert — the order is stamped with the customer id
        verify(orderRepository).save(org.mockito.ArgumentMatchers.argThat(o -> userId.equals(o.getCustomerId())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void checkout_snapshotsTheSellingVendorOntoTheOrderLine() {
        // Arrange — the line's product belongs to a vendor
        UUID vendorId = UUID.randomUUID();
        Product product = new Product(tenantId, "Widget", "widget", BigDecimal.valueOf(20));
        product.setVendorId(vendorId);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });
        when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

        // Act
        checkoutService.checkout("acme", TOKEN, request(), null, null, null);

        // Assert — the persisted line carries the vendor id
        ArgumentCaptor<List<OrderItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .satisfies(item -> assertThat(item.getVendorId()).isEqualTo(vendorId));
    }

    // ---- delivery zone / shipping -------------------------------------------

    @Test
    void checkout_withADeliveryZone_addsTheZoneFeeToShippingAndTotal() {
        // Arrange — subtotal 40, a chosen zone priced at 7.50
        UUID zoneId = UUID.randomUUID();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });
        when(shippingZoneResolver.resolveActive(tenantId, zoneId))
                .thenReturn(Optional.of(new ResolvedShippingZone("City centre", "Downtown", new BigDecimal("7.50"))));
        CheckoutRequest request = request();
        request.setDeliveryZoneId(zoneId);

        // Act
        OrderResponse response = checkoutService.checkout("acme", TOKEN, request, null, null, null);

        // Assert — total = subtotal + fee; the order snapshots the fee and the zone labels
        assertThat(response.total()).isEqualByComparingTo("47.50");
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().getShippingAmount()).isEqualByComparingTo("7.50");
        assertThat(order.getValue().getShippingAddress())
                .containsEntry("pickupPoint", "Downtown")
                .containsEntry("deliveryZone", "City centre");
    }

    @Test
    void checkout_withAnUnavailableZone_isRejected_andCreatesNoOrder() {
        // Arrange — the chosen zone (or its point) is inactive/missing
        UUID zoneId = UUID.randomUUID();
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(shippingZoneResolver.resolveActive(tenantId, zoneId)).thenReturn(Optional.empty());
        CheckoutRequest request = request();
        request.setDeliveryZoneId(zoneId);

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.checkout("acme", TOKEN, request, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("delivery zone is unavailable");
        verify(orderRepository, never()).save(any());
    }

    // ---- coupon -------------------------------------------------------------

    @Test
    void checkout_withACoupon_appliesTheDiscount_andRecordsTheRedemption() {
        // Arrange — subtotal 40, a $10 coupon → total 30
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });
        AppliedCoupon applied = new AppliedCoupon(UUID.randomUUID(), "SAVE10", new BigDecimal("10.00"));
        when(couponService.validateForCheckout(eq(tenantId), eq("SAVE10"), any(), any())).thenReturn(applied);
        CheckoutRequest request = request();
        request.setCouponCode("SAVE10");

        // Act
        OrderResponse response = checkoutService.checkout("acme", TOKEN, request, null, null, null);

        // Assert — discount applied to the total, redemption recorded against the order
        assertThat(response.total()).isEqualByComparingTo("30");
        ArgumentCaptor<Order> order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().getDiscountAmount()).isEqualByComparingTo("10.00");
        verify(couponService).recordRedemption(eq(tenantId), eq(applied), any(), any());
    }

    @Test
    void checkout_withAnInvalidCoupon_isRejected_andCreatesNoOrder() {
        // Arrange — the coupon validation throws
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        when(couponService.validateForCheckout(eq(tenantId), eq("BAD"), any(), any()))
                .thenThrow(new BusinessException("That code isn't valid"));
        CheckoutRequest request = request();
        request.setCouponCode("BAD");

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.checkout("acme", TOKEN, request, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("isn't valid");
        verify(orderRepository, never()).save(any());
        verify(stockService, never()).reserve(any(), any(), any(), anyInt());
    }

    // ---- guards -------------------------------------------------------------

    @Test
    void checkout_emptyCart_isRejected() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(new CartView(List.of(), BigDecimal.ZERO, 0, "USD"));

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.checkout("acme", TOKEN, request(), null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_insufficientStock_propagates_andCreatesNoOrder() {
        // Arrange — reservation fails for a line
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(cartService.view("acme", TOKEN)).thenReturn(cartWithOneLine());
        doThrow(new BusinessException("Not enough stock to fulfil that order"))
                .when(stockService).reserve(any(), any(), any(), anyInt());

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.checkout("acme", TOKEN, request(), null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("stock");
        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clear(any(), any());
    }

    @Test
    void checkout_hiddenStore_is404() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(false)));

        // Act & Assert
        assertThatThrownBy(() -> checkoutService.checkout("acme", TOKEN, request(), null, null, null))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- idempotency --------------------------------------------------------

    @Test
    void checkout_withSeenIdempotencyKey_returnsExistingOrder_withoutReprocessing() {
        // Arrange — an order already exists for this key
        Order existing = new Order();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setOrderNumber("ORD-EXISTING");
        existing.setSubtotal(BigDecimal.TEN);
        existing.setTotal(BigDecimal.TEN);
        existing.setCurrency("USD");
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store(true)));
        when(orderRepository.findByTenantIdAndIdempotencyKey(tenantId, "idem-1")).thenReturn(Optional.of(existing));
        when(orderItemRepository.findByOrderId(existing.getId())).thenReturn(List.of());

        // Act
        OrderResponse response = checkoutService.checkout("acme", TOKEN, request(), "idem-1", null, null);

        // Assert — returns the existing order, no new processing
        assertThat(response.orderNumber()).isEqualTo("ORD-EXISTING");
        verify(cartService, never()).view(any(), any());
        verify(stockService, never()).reserve(any(), any(), any(), anyInt());
        verify(orderRepository, never()).save(any());
    }
}
