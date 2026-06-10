package com.loyalsuit.modules.fulfilment.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.dto.AdvanceDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.AssignDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryResponse;
import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryAgentRepository;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryEventRepository;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryRepository;
import com.loyalsuit.modules.fulfilment.domain.DeliveryEvent;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Assigns orders to couriers and walks each delivery through its status lifecycle. A
 * delivery is created on first assignment (one per order) and every status change is
 * appended to its timeline. The state machine lives on the {@link Delivery} aggregate;
 * this service resolves the order/agent, snapshots their display fields, and records the
 * audit trail. Reads tenant-scoped; mutations guarded by the aggregate's optimistic lock.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventRepository eventRepository;
    private final DeliveryAgentRepository agentRepository;
    private final OrderRepository orderRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public DeliveryResponse assign(UUID tenantId, UUID actorId, AssignDeliveryRequest request) {
        Order order = orderRepository.findByOrderNumberAndTenantId(request.getOrderNumber().trim(), tenantId)
                .orElseThrow(() -> new NotFoundException("Order", request.getOrderNumber()));
        if (!isDeliverable(order.getStatus())) {
            throw new BusinessException("Order " + order.getOrderNumber() + " is not awaiting delivery");
        }

        DeliveryAgent agent = agentRepository.findByIdAndTenantId(request.getAgentId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery agent", request.getAgentId()));
        if (!agent.isActive()) {
            throw new BusinessException("That delivery agent is not active");
        }
        String agentName = agentName(agent);

        Delivery delivery = deliveryRepository.findByOrderIdAndTenantId(order.getId(), tenantId)
                .orElseGet(() -> new Delivery(tenantId, order.getId(), order.getOrderNumber(), order.getCustomerName()));
        delivery.assignTo(agent.getId(), agentName);
        Delivery saved = deliveryRepository.save(delivery);

        record(saved, DeliveryStatus.ASSIGNED, "Assigned to " + agentName, actorId);
        return DeliveryResponse.from(saved);
    }

    @Transactional
    public DeliveryResponse advance(UUID id, UUID tenantId, UUID actorId, AdvanceDeliveryRequest request) {
        return doAdvance(load(id, tenantId), actorId, request);
    }

    /** A courier's own open queue (their still-active deliveries). */
    public PageResponse<DeliveryResponse> myAssignments(UUID tenantId, UUID agentUserId, Pageable pageable) {
        DeliveryAgent agent = resolveAgent(tenantId, agentUserId);
        return new PageResponse<>(deliveryRepository.findActiveByAgent(tenantId, agent.getId(), pageable)
                .map(DeliveryResponse::from));
    }

    /** A courier advances one of their own deliveries — same lifecycle, with an ownership check. */
    @Transactional
    public DeliveryResponse advanceAsAgent(UUID id, UUID tenantId, UUID agentUserId, AdvanceDeliveryRequest request) {
        DeliveryAgent agent = resolveAgent(tenantId, agentUserId);
        Delivery delivery = load(id, tenantId);
        if (!agent.getId().equals(delivery.getAgentId())) {
            throw new BusinessException("That delivery is not assigned to you");
        }
        return doAdvance(delivery, agentUserId, request);
    }

    private DeliveryResponse doAdvance(Delivery delivery, UUID actorId, AdvanceDeliveryRequest request) {
        if (request.getStatus() == DeliveryStatus.FAILED && !StringUtils.hasText(request.getNote())) {
            throw new BusinessException("A reason is required to mark a delivery failed");
        }
        delivery.advanceTo(request.getStatus(), trimToNull(request.getNote()));
        Delivery saved = deliveryRepository.save(delivery);
        record(saved, request.getStatus(), trimToNull(request.getNote()), actorId);
        return DeliveryResponse.from(saved);
    }

    private DeliveryAgent resolveAgent(UUID tenantId, UUID agentUserId) {
        return agentRepository.findByUserId(agentUserId)
                .filter(a -> tenantId.equals(a.getTenantId()))
                .orElseThrow(() -> new NotFoundException("Delivery agent", agentUserId));
    }

    public DeliveryResponse get(UUID id, UUID tenantId) {
        Delivery delivery = load(id, tenantId);
        return DeliveryResponse.withTimeline(delivery,
                eventRepository.findByDeliveryIdAndTenantId(delivery.getId(), tenantId));
    }

    public PageResponse<DeliveryResponse> list(UUID tenantId, DeliveryStatus status, Pageable pageable) {
        var page = status == null
                ? deliveryRepository.findByTenantId(tenantId, pageable)
                : deliveryRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return new PageResponse<>(page.map(DeliveryResponse::from));
    }

    private void record(Delivery delivery, DeliveryStatus status, String note, UUID actorId) {
        eventRepository.save(new DeliveryEvent(delivery.getTenantId(), delivery.getId(), status, note, actorId));
    }

    private Delivery load(UUID id, UUID tenantId) {
        return deliveryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Delivery", id));
    }

    /** Only orders still in flight can be put out for delivery. */
    private static boolean isDeliverable(OrderStatus status) {
        return status != OrderStatus.CANCELLED
                && status != OrderStatus.REFUNDED
                && status != OrderStatus.DELIVERED;
    }

    private String agentName(DeliveryAgent agent) {
        return userRepository.findById(agent.getUserId())
                .map(user -> StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail())
                .orElse("Agent");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
