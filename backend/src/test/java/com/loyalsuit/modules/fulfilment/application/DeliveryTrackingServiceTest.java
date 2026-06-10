package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryTrackingResponse;
import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryEventRepository;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryRepository;
import com.loyalsuit.modules.orders.application.OrderTrackingService;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryTrackingServiceTest {

    @Mock private OrderTrackingService orderTrackingService;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryEventRepository eventRepository;

    @InjectMocks private DeliveryTrackingService service;

    private UUID tenantId;
    private UUID orderId;
    private UUID deliveryId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        deliveryId = UUID.randomUUID();
    }

    private OrderResponse order() {
        return new OrderResponse(orderId, "ORD-1", "PENDING", "CASH", "UNPAID",
                "Ada", "ada@x.dev", null, null, null, null, null, "USD", List.of(), null);
    }

    private Delivery delivery(DeliveryStatus status) {
        Delivery d = new Delivery(tenantId, orderId, "ORD-1", "Ada Lovelace");
        d.setStatus(status);
        d.setRecipientName(status == DeliveryStatus.DELIVERED ? "Ada Lovelace" : null);
        ReflectionTestUtils.setField(d, "id", deliveryId);
        return d;
    }

    @Test
    void track_returnsTheSanitizedTimeline_whenDispatched() {
        // Arrange — a verified order with a dispatched delivery
        when(orderTrackingService.track("acme", "ORD-1", "ada@x.dev")).thenReturn(order());
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery(DeliveryStatus.IN_TRANSIT)));
        when(eventRepository.findByDeliveryIdAndTenantId(deliveryId, tenantId)).thenReturn(List.of(
                new DeliveryEvent(tenantId, deliveryId, DeliveryStatus.ASSIGNED, "internal note", UUID.randomUUID()),
                new DeliveryEvent(tenantId, deliveryId, DeliveryStatus.PICKED_UP, null, UUID.randomUUID()),
                new DeliveryEvent(tenantId, deliveryId, DeliveryStatus.IN_TRANSIT, null, UUID.randomUUID())));

        // Act
        DeliveryTrackingResponse tracking = service.track("acme", "ORD-1", "ada@x.dev");

        // Assert — status + ordered stages; no notes/actor leak into the public shape
        assertThat(tracking.status()).isEqualTo("IN_TRANSIT");
        assertThat(tracking.timeline()).extracting(DeliveryTrackingResponse.Stage::status)
                .containsExactly("ASSIGNED", "PICKED_UP", "IN_TRANSIT");
    }

    @Test
    void track_returnsPreparing_whenTheOrderHasNoDeliveryYet() {
        // Arrange
        when(orderTrackingService.track("acme", "ORD-1", "ada@x.dev")).thenReturn(order());
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // Act
        DeliveryTrackingResponse tracking = service.track("acme", "ORD-1", "ada@x.dev");

        // Assert
        assertThat(tracking.status()).isEqualTo("PREPARING");
        assertThat(tracking.timeline()).isEmpty();
    }

    @Test
    void track_propagatesNotFound_whenTheEmailDoesNotMatch() {
        // Arrange — the order-tracking gate rejects a bad email/number with a 404
        when(orderTrackingService.track("acme", "ORD-1", "wrong@x.dev"))
                .thenThrow(new NotFoundException("Order", "ORD-1"));

        // Act & Assert
        assertThatThrownBy(() -> service.track("acme", "ORD-1", "wrong@x.dev"))
                .isInstanceOf(NotFoundException.class);
    }
}
