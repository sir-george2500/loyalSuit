package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Public order tracking for guests. A lookup must present BOTH the order number and
 * the email used at checkout; any mismatch returns the same generic 404, so the
 * endpoint can't be used to enumerate order numbers or probe whether one exists.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderTrackingService {

    private final TenantRepository tenantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderResponse track(String storeSlug, String orderNumber, String email) {
        Tenant tenant = tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Order", orderNumber));

        Order order = orderRepository.findByOrderNumberAndTenantId(orderNumber, tenant.getId())
                .filter(o -> emailMatches(o, email))
                .orElseThrow(() -> new NotFoundException("Order", orderNumber));

        return OrderResponse.from(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private static boolean emailMatches(Order order, String email) {
        return order.getCustomerEmail() != null && email != null
                && order.getCustomerEmail().trim().equalsIgnoreCase(email.trim());
    }
}
