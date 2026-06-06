package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.orders.application.dto.CreateReturnRequest;
import com.loyalsuit.modules.orders.application.dto.ReturnResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.ReturnRequest;
import com.loyalsuit.modules.orders.domain.ReturnStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.orders.domain.port.ReturnRequestRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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
class ReturnServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ReturnRequestRepository returnRepository;
    @Mock private StockService stockService;

    @InjectMocks private ReturnService service;

    private UUID tenantId;
    private UUID orderId;
    private UUID returnId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        returnId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Tenant store() {
        Tenant t = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(t, "id", tenantId);
        t.setActive(true);
        return t;
    }

    private Order order(OrderStatus status, String email) {
        Order o = new Order();
        ReflectionTestUtils.setField(o, "id", orderId);
        o.setOrderNumber("ORD-1");
        o.setStatus(status);
        o.setPaymentStatus(PaymentStatus.PAID);
        o.setCustomerEmail(email);
        o.setSubtotal(BigDecimal.TEN);
        o.setTotal(BigDecimal.TEN);
        o.setCurrency("USD");
        return o;
    }

    private CreateReturnRequest request(String email) {
        var r = new CreateReturnRequest();
        r.setEmail(email);
        r.setReason("Wrong size");
        return r;
    }

    private ReturnRequest pendingReturn() {
        ReturnRequest r = new ReturnRequest(tenantId, orderId, "ORD-1", "Wrong size", "jane@acme.dev");
        ReflectionTestUtils.setField(r, "id", returnId);
        return r;
    }

    // ---- request ------------------------------------------------------------

    @Test
    void requestReturn_createsRequestForDeliveredOrder() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order(OrderStatus.DELIVERED, "jane@acme.dev")));
        when(returnRepository.existsByOrderIdAndStatus(orderId, ReturnStatus.REQUESTED)).thenReturn(false);
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReturnResponse response = service.requestReturn("acme", "ORD-1", request("jane@acme.dev"));

        // Assert
        assertThat(response.status()).isEqualTo("REQUESTED");
        assertThat(response.orderNumber()).isEqualTo("ORD-1");
    }

    @Test
    void requestReturn_404_whenEmailDoesNotMatch() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order(OrderStatus.DELIVERED, "jane@acme.dev")));

        // Act & Assert — no leak
        assertThatThrownBy(() -> service.requestReturn("acme", "ORD-1", request("attacker@evil.dev")))
                .isInstanceOf(NotFoundException.class);
        verify(returnRepository, never()).save(any());
    }

    @Test
    void requestReturn_rejectsOrderNotYetDelivered() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order(OrderStatus.PROCESSING, "jane@acme.dev")));

        // Act & Assert
        assertThatThrownBy(() -> service.requestReturn("acme", "ORD-1", request("jane@acme.dev")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("delivered");
        verify(returnRepository, never()).save(any());
    }

    @Test
    void requestReturn_rejectsWhenOneIsAlreadyPending() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order(OrderStatus.DELIVERED, "jane@acme.dev")));
        when(returnRepository.existsByOrderIdAndStatus(orderId, ReturnStatus.REQUESTED)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.requestReturn("acme", "ORD-1", request("jane@acme.dev")))
                .isInstanceOf(ConflictException.class);
        verify(returnRepository, never()).save(any());
    }

    // ---- approve ------------------------------------------------------------

    @Test
    void approve_refundsOrder_restocksItems_andMarksApproved() {
        // Arrange
        Order order = order(OrderStatus.DELIVERED, "jane@acme.dev");
        when(returnRepository.findByIdAndTenantId(returnId, tenantId)).thenReturn(Optional.of(pendingReturn()));
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(2);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReturnResponse response = service.approve(returnId, tenantId);

        // Assert
        assertThat(response.status()).isEqualTo("APPROVED");
        verify(orderRepository).save(ArgumentMatchers.argThat(o ->
                o.getStatus() == OrderStatus.REFUNDED && o.getPaymentStatus() == PaymentStatus.REFUNDED));
        verify(stockService).release(tenantId, productId, null, 2);
    }

    @Test
    void approve_rejectsAnAlreadyResolvedReturn() {
        // Arrange
        ReturnRequest resolved = pendingReturn();
        resolved.setStatus(ReturnStatus.APPROVED);
        when(returnRepository.findByIdAndTenantId(returnId, tenantId)).thenReturn(Optional.of(resolved));

        // Act & Assert
        assertThatThrownBy(() -> service.approve(returnId, tenantId)).isInstanceOf(ConflictException.class);
        verify(orderRepository, never()).save(any());
    }

    // ---- reject -------------------------------------------------------------

    @Test
    void reject_marksRejectedWithNote() {
        // Arrange
        when(returnRepository.findByIdAndTenantId(returnId, tenantId)).thenReturn(Optional.of(pendingReturn()));
        when(returnRepository.save(any(ReturnRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReturnResponse response = service.reject(returnId, tenantId, "Outside return window");

        // Assert
        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.resolutionNote()).isEqualTo("Outside return window");
    }
}
