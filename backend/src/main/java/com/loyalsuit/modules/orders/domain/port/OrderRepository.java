package com.loyalsuit.modules.orders.domain.port;

import com.loyalsuit.modules.orders.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Order> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    boolean existsByOrderNumber(String orderNumber);
}
