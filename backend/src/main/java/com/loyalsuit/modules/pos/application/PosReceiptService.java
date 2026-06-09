package com.loyalsuit.modules.pos.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.pos.application.dto.ReceiptView;
import com.loyalsuit.modules.pos.domain.PosSale;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import com.loyalsuit.modules.pos.infrastructure.receipt.ReceiptPdfRenderer;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds a printable PDF receipt for a completed POS sale. The sale row holds the money
 * and receipt number; the line items live on the linked order, and product names are
 * resolved from the current catalog (order lines don't snapshot names — a since-deleted
 * product falls back to a placeholder). Read-only; tenant-scoped on every lookup.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PosReceiptService {

    private final PosSaleRepository saleRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final ReceiptPdfRenderer renderer;

    /** A rendered receipt: the PDF bytes plus a download filename built from the receipt number. */
    public record RenderedReceipt(String fileName, byte[] content) {
    }

    public RenderedReceipt renderReceipt(UUID tenantId, UUID saleId) {
        PosSale sale = saleRepository.findByIdAndTenantId(saleId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sale", saleId));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Store", tenantId));

        List<OrderItem> items = orderItemRepository.findByOrderId(sale.getOrderId());
        Map<UUID, String> names = productNames(items, tenantId);

        List<ReceiptView.Line> lines = items.stream()
                .map(item -> new ReceiptView.Line(
                        names.getOrDefault(item.getProductId(), "Item"),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotal()))
                .toList();

        ReceiptView view = new ReceiptView(
                tenant.getName(),
                tenant.getCurrency(),
                zoneOf(tenant.getTimezone()),
                sale.getOrderNumber(),
                sale.getCreatedAt(),
                cashierName(sale.getCashierId()),
                lines,
                sale.getSubtotal(),
                sale.getTotal(),
                sale.getAmountTendered(),
                sale.getChangeGiven(),
                sale.getCardAmount(),
                sale.getItemCount());

        return new RenderedReceipt("receipt-" + sale.getOrderNumber() + ".pdf", renderer.render(view));
    }

    private Map<UUID, String> productNames(List<OrderItem> items, UUID tenantId) {
        List<UUID> ids = items.stream().map(OrderItem::getProductId).distinct().toList();
        return productRepository.findByIdInAndTenantId(ids, tenantId).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
    }

    /** The cashier's display name (full name, else email), or null if they no longer exist. */
    private String cashierName(UUID cashierId) {
        return userRepository.findById(cashierId)
                .map(user -> StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getEmail())
                .orElse(null);
    }

    private static ZoneId zoneOf(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
