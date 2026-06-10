package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.modules.fulfilment.application.dto.DeliveryTrackingResponse;
import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryEventRepository;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryRepository;
import com.loyalsuit.modules.orders.application.OrderTrackingService;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Public, customer-facing delivery tracking. It reuses {@link OrderTrackingService} to
 * authenticate the lookup — the same order-number + email gate, so a wrong email is the
 * same generic 404 and delivery progress can't be probed by guessing order numbers. Once
 * the order is verified, its delivery (if dispatched) is returned in sanitized form.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryTrackingService {

    private final OrderTrackingService orderTrackingService;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventRepository eventRepository;

    public DeliveryTrackingResponse track(String storeSlug, String orderNumber, String email) {
        // Verifies the order number + email (or throws the generic 404).
        OrderResponse order = orderTrackingService.track(storeSlug, orderNumber, email);

        Optional<Delivery> delivery = deliveryRepository.findByOrderId(order.id());
        return delivery
                .map(d -> DeliveryTrackingResponse.from(d,
                        eventRepository.findByDeliveryIdAndTenantId(d.getId(), d.getTenantId())))
                .orElseGet(() -> DeliveryTrackingResponse.preparing(orderNumber));
    }
}
