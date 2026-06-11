package com.loyalsuit.modules.orders.domain.port;

import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Order> findByOrderNumberAndTenantId(String orderNumber, UUID tenantId);
    Optional<Order> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    boolean existsByOrderNumber(String orderNumber);
    Page<Order> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable);

    /** A customer's own orders — for their personal-data export and erasure. */
    List<Order> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
