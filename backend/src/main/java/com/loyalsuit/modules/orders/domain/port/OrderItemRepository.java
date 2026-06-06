package com.loyalsuit.modules.orders.domain.port;

import com.loyalsuit.modules.orders.domain.OrderItem;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository {
    List<OrderItem> saveAll(List<OrderItem> items);
    List<OrderItem> findByOrderId(UUID orderId);
}
