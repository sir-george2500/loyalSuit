package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.fulfilment.application.dto.AdvanceDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.AssignDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryResponse;
import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import com.loyalsuit.modules.fulfilment.domain.VehicleType;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryAgentRepository;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryEventRepository;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryRepository;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryEventRepository eventRepository;
    @Mock private DeliveryAgentRepository agentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks private DeliveryService service;

    private UUID tenantId;
    private UUID actorId;
    private UUID orderId;
    private UUID agentId;
    private UUID agentUserId;
    private UUID deliveryId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        agentUserId = UUID.randomUUID();
        deliveryId = UUID.randomUUID();
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber("ORD-1001");
        order.setCustomerName("Ada Lovelace");
        order.setStatus(status);
        order.setSubtotal(java.math.BigDecimal.TEN);
        order.setTotal(java.math.BigDecimal.TEN);
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    private DeliveryAgent agent(boolean active) {
        DeliveryAgent agent = new DeliveryAgent(tenantId, agentUserId, "555", VehicleType.MOTORBIKE);
        agent.setActive(active);
        ReflectionTestUtils.setField(agent, "id", agentId);
        return agent;
    }

    private Delivery delivery(DeliveryStatus status) {
        Delivery delivery = new Delivery(tenantId, orderId, "ORD-1001", "Ada Lovelace");
        delivery.setStatus(status);
        delivery.setAgentId(agentId);
        ReflectionTestUtils.setField(delivery, "id", deliveryId);
        return delivery;
    }

    private AssignDeliveryRequest assignRequest() {
        AssignDeliveryRequest request = new AssignDeliveryRequest();
        request.setOrderNumber("ORD-1001");
        request.setAgentId(agentId);
        return request;
    }

    private AdvanceDeliveryRequest advanceTo(DeliveryStatus status, String note) {
        AdvanceDeliveryRequest request = new AdvanceDeliveryRequest();
        request.setStatus(status);
        request.setNote(note);
        return request;
    }

    private void stubDeliverySave() {
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> {
            Delivery d = inv.getArgument(0);
            if (ReflectionTestUtils.getField(d, "id") == null) {
                ReflectionTestUtils.setField(d, "id", deliveryId);
            }
            return d;
        });
    }

    private void stubAgentUser() {
        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(
                new AppUser(tenantId, "rider@store.dev", "hash", "Rider Rick", UserRole.DELIVERY_AGENT)));
    }

    // ---- assign -------------------------------------------------------------

    @Test
    void assign_createsAnAssignedDelivery_andRecordsTheEvent() {
        // Arrange — a deliverable order, an active agent, no existing delivery
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1001", tenantId)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));
        when(agentRepository.findByIdAndTenantId(agentId, tenantId)).thenReturn(Optional.of(agent(true)));
        when(deliveryRepository.findByOrderIdAndTenantId(orderId, tenantId)).thenReturn(Optional.empty());
        stubAgentUser();
        stubDeliverySave();

        // Act
        DeliveryResponse delivery = service.assign(tenantId, actorId, assignRequest());

        // Assert
        assertThat(delivery.status()).isEqualTo("ASSIGNED");
        assertThat(delivery.agentId()).isEqualTo(agentId);
        assertThat(delivery.agentName()).isEqualTo("Rider Rick");
        assertThat(delivery.assignedAt()).isNotNull();
        ArgumentCaptor<DeliveryEvent> event = ArgumentCaptor.forClass(DeliveryEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
    }

    @Test
    void assign_isRejected_whenTheOrderIsNotAwaitingDelivery() {
        // Arrange — an already-delivered order (e.g. a POS sale)
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1001", tenantId)).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));

        // Act & Assert
        assertThatThrownBy(() -> service.assign(tenantId, actorId, assignRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not awaiting delivery");
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void assign_isRejected_whenTheAgentIsInactive() {
        // Arrange
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1001", tenantId)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));
        when(agentRepository.findByIdAndTenantId(agentId, tenantId)).thenReturn(Optional.of(agent(false)));

        // Act & Assert
        assertThatThrownBy(() -> service.assign(tenantId, actorId, assignRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void assign_aMissingOrder_is404() {
        // Arrange
        when(orderRepository.findByOrderNumberAndTenantId("ORD-1001", tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.assign(tenantId, actorId, assignRequest()))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- advance ------------------------------------------------------------

    @Test
    void advance_movesAlongTheLifecycle_andStampsTheTimestamp() {
        // Arrange — an ASSIGNED delivery → PICKED_UP
        when(deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)).thenReturn(Optional.of(delivery(DeliveryStatus.ASSIGNED)));
        stubDeliverySave();

        // Act
        DeliveryResponse delivery = service.advance(deliveryId, tenantId, actorId, advanceTo(DeliveryStatus.PICKED_UP, null));

        // Assert
        assertThat(delivery.status()).isEqualTo("PICKED_UP");
        assertThat(delivery.pickedUpAt()).isNotNull();
        verify(eventRepository).save(any(DeliveryEvent.class));
    }

    @Test
    void advance_anIllegalTransition_isRejected() {
        // Arrange — ASSIGNED can't jump straight to DELIVERED
        when(deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)).thenReturn(Optional.of(delivery(DeliveryStatus.ASSIGNED)));

        // Act & Assert
        assertThatThrownBy(() -> service.advance(deliveryId, tenantId, actorId, advanceTo(DeliveryStatus.DELIVERED, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("can't move");
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void advance_toFailed_requiresAReason() {
        // Arrange
        when(deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)).thenReturn(Optional.of(delivery(DeliveryStatus.IN_TRANSIT)));

        // Act & Assert
        assertThatThrownBy(() -> service.advance(deliveryId, tenantId, actorId, advanceTo(DeliveryStatus.FAILED, "  ")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reason is required");
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void advance_toFailed_recordsTheReason() {
        // Arrange
        when(deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)).thenReturn(Optional.of(delivery(DeliveryStatus.IN_TRANSIT)));
        stubDeliverySave();

        // Act
        DeliveryResponse delivery = service.advance(
                deliveryId, tenantId, actorId, advanceTo(DeliveryStatus.FAILED, "Customer unreachable"));

        // Assert
        assertThat(delivery.status()).isEqualTo("FAILED");
        assertThat(delivery.failureReason()).isEqualTo("Customer unreachable");
        assertThat(delivery.failedAt()).isNotNull();
    }

    // ---- get ----------------------------------------------------------------

    @Test
    void get_returnsTheDelivery_withItsTimeline() {
        // Arrange
        when(deliveryRepository.findByIdAndTenantId(deliveryId, tenantId)).thenReturn(Optional.of(delivery(DeliveryStatus.PICKED_UP)));
        when(eventRepository.findByDeliveryIdAndTenantId(deliveryId, tenantId)).thenReturn(List.of(
                new DeliveryEvent(tenantId, deliveryId, DeliveryStatus.ASSIGNED, "Assigned to Rider Rick", actorId),
                new DeliveryEvent(tenantId, deliveryId, DeliveryStatus.PICKED_UP, null, actorId)));

        // Act
        DeliveryResponse delivery = service.get(deliveryId, tenantId);

        // Assert
        assertThat(delivery.events()).hasSize(2);
        assertThat(delivery.events().get(0).status()).isEqualTo("ASSIGNED");
    }
}
