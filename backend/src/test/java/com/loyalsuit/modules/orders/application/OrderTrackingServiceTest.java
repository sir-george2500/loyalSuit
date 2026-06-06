package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTrackingServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks private OrderTrackingService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private Tenant store() {
        Tenant t = new Tenant("Acme", "acme");
        ReflectionTestUtils.setField(t, "id", tenantId);
        t.setActive(true);
        t.setCurrency("USD");
        return t;
    }

    private Order order(String email) {
        Order o = new Order();
        ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
        o.setOrderNumber("ORD-1");
        o.setCustomerName("Jane");
        o.setCustomerEmail(email);
        o.setSubtotal(BigDecimal.TEN);
        o.setTotal(BigDecimal.TEN);
        o.setCurrency("USD");
        return o;
    }

    @Test
    void track_returnsOrder_whenNumberAndEmailMatch() {
        // Arrange — email match is case-insensitive and trims whitespace
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order("jane@acme.dev")));
        when(orderItemRepository.findByOrderId(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        // Act
        OrderResponse response = service.track("acme", "ORD-1", "  Jane@Acme.dev ");

        // Assert
        assertThat(response.orderNumber()).isEqualTo("ORD-1");
    }

    @Test
    void track_404_whenEmailDoesNotMatch_withoutRevealingTheOrderExists() {
        // Arrange — order exists but the email is wrong
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order("jane@acme.dev")));

        // Act & Assert — same generic 404 as a missing order
        assertThatThrownBy(() -> service.track("acme", "ORD-1", "attacker@evil.dev"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void track_404_whenOrderNumberUnknown() {
        // Arrange
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-X", tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.track("acme", "ORD-X", "jane@acme.dev"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void track_404_whenStoreIsHidden() {
        // Arrange
        Tenant suspended = store();
        suspended.setActive(false);
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(suspended));

        // Act & Assert
        assertThatThrownBy(() -> service.track("acme", "ORD-1", "jane@acme.dev"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void track_404_whenOrderHasNoEmailOnFile() {
        // Arrange — a guest order placed without an email can't be tracked by email
        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(store()));
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1", tenantId))
                .thenReturn(Optional.of(order(null)));

        // Act & Assert
        assertThatThrownBy(() -> service.track("acme", "ORD-1", "jane@acme.dev"))
                .isInstanceOf(NotFoundException.class);
    }
}
