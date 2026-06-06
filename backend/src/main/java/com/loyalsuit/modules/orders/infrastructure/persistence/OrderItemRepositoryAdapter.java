package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryAdapter implements OrderItemRepository {

    private final OrderItemJpaRepository jpa;

    @Override
    public List<OrderItem> saveAll(List<OrderItem> items) {
        return jpa.saveAll(items);
    }

    @Override
    public List<OrderItem> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId);
    }
}
