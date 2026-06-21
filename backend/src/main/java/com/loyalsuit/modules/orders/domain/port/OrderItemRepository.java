package com.loyalsuit.modules.orders.domain.port;

import com.loyalsuit.modules.orders.domain.OrderItem;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository {
    List<OrderItem> saveAll(List<OrderItem> items);
    List<OrderItem> findByOrderId(UUID orderId);
    /** A vendor's own lines across the given orders — for the seller's order view (one query). */
    List<OrderItem> findByOrderIdInAndVendorId(Collection<UUID> orderIds, UUID vendorId);

    /**
     * Lines for a given product that a customer has received (their order is DELIVERED).
     * Used to verify a purchase before allowing a product review.
     */
    List<OrderItem> findDeliveredItems(UUID tenantId, UUID customerId, UUID productId);
}
