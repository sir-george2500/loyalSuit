package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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
    Page<Order> findByTenantIdAndPaymentStatusOrderByCreatedAtDesc(UUID tenantId, PaymentStatus paymentStatus, Pageable pageable);
    List<Order> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    // Orders holding at least one line for this vendor. OrderItem isn't mapped as a relationship,
    // so we join on its order_id. DISTINCT collapses multi-line orders to one row.
    @Query(value = """
            SELECT DISTINCT o FROM Order o, OrderItem i
            WHERE o.tenantId = :tenantId AND i.orderId = o.id AND i.vendorId = :vendorId
            ORDER BY o.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o.id) FROM Order o, OrderItem i
            WHERE o.tenantId = :tenantId AND i.orderId = o.id AND i.vendorId = :vendorId
            """)
    Page<Order> findOrdersForVendor(
            @Param("tenantId") UUID tenantId, @Param("vendorId") UUID vendorId, Pageable pageable);

    // Returns rows of [customerId(UUID), count(Long), spend(BigDecimal)]; mapped in the adapter
    // to stay dialect-agnostic about how COUNT/SUM widen into a record constructor.
    @Query("""
            SELECT o.customerId, COUNT(o), COALESCE(SUM(o.total), 0)
            FROM Order o
            WHERE o.tenantId = :tenantId AND o.customerId IN :customerIds
            GROUP BY o.customerId
            """)
    List<Object[]> aggregateOrdersByCustomer(
            @Param("tenantId") UUID tenantId, @Param("customerIds") Collection<UUID> customerIds);
}
