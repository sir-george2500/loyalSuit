package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.application.dto.VendorOrderItemResponse;
import com.loyalsuit.modules.orders.application.dto.VendorOrderResponse;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A vendor's view of orders: the orders that contain their products, showing only their own lines
 * and earnings. A vendor never sees another seller's lines or the order's grand total. The caller
 * supplies the resolved vendor id (the Vendor PK), so this service stays free of vendor identity.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VendorOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public PageResponse<VendorOrderResponse> list(UUID tenantId, UUID vendorId, Pageable pageable) {
        var page = orderRepository.findOrdersForVendor(tenantId, vendorId, pageable);

        List<UUID> orderIds = page.getContent().stream().map(Order::getId).toList();
        // Empty order ids → repository returns no lines, so an empty page maps cleanly to empty.
        List<OrderItem> lines = orderItemRepository.findByOrderIdInAndVendorId(orderIds, vendorId);
        Map<UUID, List<OrderItem>> linesByOrder = lines.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Map<UUID, String> productNames = productNames(tenantId, lines);

        return new PageResponse<>(page.map(order ->
                toResponse(order, linesByOrder.getOrDefault(order.getId(), List.of()), productNames)));
    }

    public VendorOrderResponse get(UUID tenantId, UUID vendorId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new NotFoundException("Order", orderId));
        List<OrderItem> lines = orderItemRepository.findByOrderIdInAndVendorId(List.of(orderId), vendorId);
        if (lines.isEmpty()) {
            // The order exists but holds nothing this vendor sold — not theirs to see.
            throw new NotFoundException("Order", orderId);
        }
        return toResponse(order, lines, productNames(tenantId, lines));
    }

    private Map<UUID, String> productNames(UUID tenantId, List<OrderItem> lines) {
        List<UUID> productIds = lines.stream().map(OrderItem::getProductId).distinct().toList();
        return productRepository.findByIdInAndTenantId(productIds, tenantId).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
    }

    private VendorOrderResponse toResponse(Order order, List<OrderItem> lines, Map<UUID, String> productNames) {
        BigDecimal vendorSubtotal = lines.stream()
                .map(OrderItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<VendorOrderItemResponse> items = lines.stream()
                .map(i -> new VendorOrderItemResponse(
                        i.getProductId(),
                        productNames.getOrDefault(i.getProductId(), "Product"),
                        i.getVariantId(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotal()))
                .toList();
        return new VendorOrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getCustomerName(),
                order.getCurrency(),
                vendorSubtotal,
                items,
                order.getCreatedAt());
    }
}
