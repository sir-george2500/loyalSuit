package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    @Override
    public Order save(Order order) {
        return jpa.save(order);
    }

    @Override
    public Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<Order> findByOrderNumberAndTenantId(String orderNumber, UUID tenantId) {
        return jpa.findByOrderNumberAndTenantId(orderNumber, tenantId);
    }

    @Override
    public Optional<Order> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return jpa.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);
    }

    @Override
    public boolean existsByOrderNumber(String orderNumber) {
        return jpa.existsByOrderNumber(orderNumber);
    }

    @Override
    public Page<Order> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<Order> findByTenantIdAndStatus(UUID tenantId, OrderStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }

    @Override
    public List<Order> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return jpa.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId);
    }
}
