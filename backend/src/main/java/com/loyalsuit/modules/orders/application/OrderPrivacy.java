package com.loyalsuit.modules.orders.application;

import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.privacy.domain.port.PersonalDataContributor;
import com.loyalsuit.modules.privacy.domain.port.PersonalDataEraser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The orders module's GDPR hooks. Orders are financial records, so erasure keeps the row but
 * scrubs the customer's personal fields (name / email / phone); export returns the customer's
 * own orders.
 */
@Component
@RequiredArgsConstructor
public class OrderPrivacy implements PersonalDataContributor, PersonalDataEraser {

    private final OrderRepository orderRepository;

    @Override
    public String section() {
        return "orders";
    }

    @Override
    public Object export(UUID userId, UUID tenantId) {
        return orderRepository.findByTenantIdAndCustomerId(tenantId, userId).stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    @Transactional
    public void erase(UUID userId, UUID tenantId) {
        for (Order order : orderRepository.findByTenantIdAndCustomerId(tenantId, userId)) {
            order.setCustomerName("Deleted user");
            order.setCustomerEmail(null);
            order.setCustomerPhone(null);
            orderRepository.save(order);
        }
    }

    private Map<String, Object> toRow(Order order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("orderNumber", order.getOrderNumber());
        row.put("status", order.getStatus().name());
        row.put("total", order.getTotal());
        row.put("placedAt", String.valueOf(order.getCreatedAt()));
        return row;
    }
}
