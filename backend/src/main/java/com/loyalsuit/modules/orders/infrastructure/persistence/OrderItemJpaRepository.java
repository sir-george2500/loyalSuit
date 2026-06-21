package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface OrderItemJpaRepository extends JpaRepository<OrderItem, UUID> {
    List<OrderItem> findByOrderId(UUID orderId);
    List<OrderItem> findByOrderIdInAndVendorId(Collection<UUID> orderIds, UUID vendorId);

    // OrderItem isn't mapped as a relationship on Order, so join on its order_id. A purchase
    // counts only once the order is DELIVERED.
    @Query("""
            SELECT i FROM Order o, OrderItem i
            WHERE o.tenantId = :tenantId AND i.orderId = o.id
              AND o.customerId = :customerId AND i.productId = :productId
              AND o.status = com.loyalsuit.modules.orders.domain.OrderStatus.DELIVERED
            """)
    List<OrderItem> findDeliveredItems(
            @Param("tenantId") UUID tenantId,
            @Param("customerId") UUID customerId,
            @Param("productId") UUID productId);
}
