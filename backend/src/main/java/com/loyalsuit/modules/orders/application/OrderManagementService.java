package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.modules.orders.application.dto.OrderSummaryResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin order management for a tenant: browse orders, drive the fulfilment state
 * machine (guarded transitions), release stock on cancellation, and record cash
 * payment. All operations are tenant-scoped.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockService stockService;

    public PageResponse<OrderSummaryResponse> list(UUID tenantId, OrderStatus status, Pageable pageable) {
        var page = status == null
                ? orderRepository.findByTenantId(tenantId, pageable)
                : orderRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return new PageResponse<>(page.map(OrderSummaryResponse::from));
    }

    public OrderResponse get(UUID id, UUID tenantId) {
        Order order = load(id, tenantId);
        return OrderResponse.from(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional
    public OrderResponse transition(UUID id, UUID tenantId, OrderStatus target) {
        Order order = load(id, tenantId);

        if (!order.getStatus().canTransitionTo(target)) {
            throw new BusinessException(
                    "Can't move an order from " + order.getStatus() + " to " + target);
        }

        // Cancelling an order that still holds reserved stock returns it to inventory.
        if (target == OrderStatus.CANCELLED && order.getStatus().holdsReservedStock()) {
            for (OrderItem item : orderItemRepository.findByOrderId(order.getId())) {
                stockService.release(tenantId, item.getProductId(), item.getVariantId(), item.getQuantity());
            }
        }

        order.setStatus(target);
        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    @Transactional
    public OrderResponse markPaid(UUID id, UUID tenantId) {
        Order order = load(id, tenantId);
        if (order.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new BusinessException("This order is already marked " + order.getPaymentStatus());
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        Order saved = orderRepository.save(order);
        return OrderResponse.from(saved, orderItemRepository.findByOrderId(saved.getId()));
    }

    private Order load(UUID id, UUID tenantId) {
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Order", id));
    }
}
