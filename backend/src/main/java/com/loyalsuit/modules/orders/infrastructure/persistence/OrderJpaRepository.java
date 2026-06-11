package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Order> findByOrderNumberAndTenantId(String orderNumber, UUID tenantId);
    Optional<Order> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    boolean existsByOrderNumber(String orderNumber);
    Page<Order> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Order> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, OrderStatus status, Pageable pageable);
    List<Order> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);
}
