package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.marketplace.application.CommissionLine;
import com.loyalsuit.modules.marketplace.application.CommissionService;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderManagementServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private StockService stockService;
    @Mock private CommissionService commissionService;
    @Mock private com.loyalsuit.modules.loyalty.application.LoyaltyService loyaltyService;

    @InjectMocks private OrderManagementService service;

    private UUID tenantId;
    private UUID orderId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Order order(OrderStatus status, PaymentStatus payment) {
        Order o = new Order();
        ReflectionTestUtils.setField(o, "id", orderId);
        o.setTenantId(tenantId);
        o.setOrderNumber("ORD-1");
        o.setStatus(status);
        o.setPaymentStatus(payment);
        o.setSubtotal(BigDecimal.TEN);
        o.setTotal(BigDecimal.TEN);
        o.setCurrency("USD");
        return o;
    }

    private OrderItem item(int quantity) {
        OrderItem i = new OrderItem();
        i.setProductId(productId);
        i.setQuantity(quantity);
        i.setUnitPrice(BigDecimal.valueOf(5));
        i.setTotal(BigDecimal.valueOf(5 * quantity));
        return i;
    }

    // ---- state machine ------------------------------------------------------

    @Test
    void transition_followsAValidEdge() {
        // Arrange
        Order order = order(OrderStatus.PENDING, PaymentStatus.UNPAID);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        // Act
        OrderResponse response = service.transition(orderId, tenantId, OrderStatus.CONFIRMED);

        // Assert
        assertThat(response.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void transition_rejectsAnInvalidEdge() {
        // Arrange — PENDING can't jump straight to SHIPPED
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order(OrderStatus.PENDING, PaymentStatus.UNPAID)));

        // Act & Assert
        assertThatThrownBy(() -> service.transition(orderId, tenantId, OrderStatus.SHIPPED))
                .isInstanceOf(BusinessException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void transition_cannotCancelAShippedOrder() {
        // Arrange — SHIPPED only advances to DELIVERED
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order(OrderStatus.SHIPPED, PaymentStatus.UNPAID)));

        // Act & Assert
        assertThatThrownBy(() -> service.transition(orderId, tenantId, OrderStatus.CANCELLED))
                .isInstanceOf(BusinessException.class);
        verify(stockService, never()).release(any(), any(), any(), anyInt());
    }

    @Test
    void transition_cancel_releasesReservedStock() {
        // Arrange — cancelling a PENDING order returns its items to stock
        Order order = order(OrderStatus.PENDING, PaymentStatus.UNPAID);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item(2)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderResponse response = service.transition(orderId, tenantId, OrderStatus.CANCELLED);

        // Assert
        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(stockService).release(tenantId, productId, null, 2);
    }

    // ---- payment ------------------------------------------------------------

    @Test
    void markPaid_setsPaymentToPaid() {
        // Arrange
        Order order = order(OrderStatus.CONFIRMED, PaymentStatus.UNPAID);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        // Act
        OrderResponse response = service.markPaid(orderId, tenantId);

        // Assert
        assertThat(response.paymentStatus()).isEqualTo("PAID");
    }

    @Test
    @SuppressWarnings("unchecked")
    void markPaid_settlesCommissionForVendorLinesOnly() {
        // Arrange — a delivered order with one vendor line and one house (no-vendor) line
        Order order = order(OrderStatus.DELIVERED, PaymentStatus.UNPAID);
        UUID vendorId = UUID.randomUUID();
        OrderItem vendorItem = item(2);
        vendorItem.setVendorId(vendorId);
        ReflectionTestUtils.setField(vendorItem, "id", UUID.randomUUID());
        OrderItem houseItem = item(1); // no vendor — excluded from settlement
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(vendorItem, houseItem));

        // Act
        service.markPaid(orderId, tenantId);

        // Assert — only the vendor line is handed to the commission engine
        ArgumentCaptor<List<CommissionLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionService).settleOrder(eq(tenantId), eq(orderId), eq("ORD-1"), captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(line -> {
            assertThat(line.vendorId()).isEqualTo(vendorId);
            assertThat(line.grossAmount()).isEqualByComparingTo("10");
        });
    }

    @Test
    void markPaid_rejectsAnAlreadyPaidOrder() {
        // Arrange
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order(OrderStatus.CONFIRMED, PaymentStatus.PAID)));

        // Act & Assert
        assertThatThrownBy(() -> service.markPaid(orderId, tenantId))
                .isInstanceOf(BusinessException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void settleCashOnDelivery_paysAndSettlesAnUnpaidOrder() {
        // Arrange
        Order order = order(OrderStatus.PENDING, PaymentStatus.UNPAID);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        // Act
        service.settleCashOnDelivery(orderId, tenantId);

        // Assert
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(commissionService).settleOrder(eq(tenantId), eq(orderId), eq("ORD-1"), any());
    }

    @Test
    void settleCashOnDelivery_isANoOp_whenAlreadyPaid() {
        // Arrange — already settled (e.g. a re-run); must not pay or settle twice
        when(orderRepository.findByIdAndTenantId(orderId, tenantId))
                .thenReturn(Optional.of(order(OrderStatus.CONFIRMED, PaymentStatus.PAID)));

        // Act
        service.settleCashOnDelivery(orderId, tenantId);

        // Assert
        verify(orderRepository, never()).save(any());
        verify(commissionService, never()).settleOrder(any(), any(), any(), any());
    }

    @Test
    void get_throwsNotFound_whenOrderNotInTenant() {
        // Arrange
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.get(orderId, tenantId)).isInstanceOf(NotFoundException.class);
    }
}
