package com.loyalsuit.modules.billing.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.billing.application.dto.InvoiceLine;
import com.loyalsuit.modules.billing.application.dto.InvoiceResponse;
import com.loyalsuit.modules.billing.application.dto.InvoiceSummaryResponse;
import com.loyalsuit.modules.billing.application.dto.PaymentResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Billing is a read model over orders: a "payment" mirrors an order's payment fields, and an
 * "invoice" renders an order plus its line items. There is no separate billing store — orders
 * are the source of truth, so figures always reconcile with fulfilment.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final String INVOICE_PREFIX = "INV-";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listPayments(UUID tenantId, PaymentStatus status, Pageable pageable) {
        Page<Order> orders = (status == null)
                ? orderRepository.findByTenantId(tenantId, pageable)
                : orderRepository.findByTenantIdAndPaymentStatus(tenantId, status, pageable);
        return new PageResponse<>(orders.map(o -> new PaymentResponse(
                o.getId(), o.getOrderNumber(), o.getCustomerName(),
                o.getPaymentMethod(), o.getPaymentStatus(), o.getTotal(), o.getCurrency(), o.getCreatedAt())));
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceSummaryResponse> listInvoices(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(orderRepository.findByTenantId(tenantId, pageable).map(o -> new InvoiceSummaryResponse(
                o.getId(), invoiceNumber(o), o.getOrderNumber(), o.getCustomerName(),
                o.getPaymentStatus(), o.getTotal(), o.getCurrency(), o.getCreatedAt())));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID tenantId, UUID orderId) {
        Order order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new NotFoundException("Invoice", orderId));

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<UUID> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<UUID, String> names = productRepository.findByIdInAndTenantId(productIds, tenantId).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<InvoiceLine> lines = items.stream()
                .map(i -> new InvoiceLine(
                        names.getOrDefault(i.getProductId(), "Item"),
                        i.getQuantity(), i.getUnitPrice(), i.getTotal()))
                .toList();

        return new InvoiceResponse(
                order.getId(), invoiceNumber(order), order.getOrderNumber(), order.getCreatedAt(),
                order.getCustomerName(), order.getCustomerEmail(), order.getCustomerPhone(),
                order.getPaymentMethod(), order.getPaymentStatus(), order.getCurrency(),
                lines, order.getSubtotal(), order.getShippingAmount(), order.getTaxAmount(),
                order.getDiscountAmount(), order.getTotal());
    }

    private String invoiceNumber(Order order) {
        return INVOICE_PREFIX + order.getOrderNumber();
    }
}
