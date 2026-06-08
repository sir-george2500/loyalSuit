package com.loyalsuit.modules.pos.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.ProductStatus;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.marketplace.application.CommissionLine;
import com.loyalsuit.modules.marketplace.application.CommissionService;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentMethod;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.pos.application.dto.CompleteSaleRequest;
import com.loyalsuit.modules.pos.application.dto.PosProductResponse;
import com.loyalsuit.modules.pos.application.dto.PosSaleResponse;
import com.loyalsuit.modules.pos.application.dto.SaleLineRequest;
import com.loyalsuit.modules.pos.domain.PosSale;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The point-of-sale engine: a cashier searches the catalog, rings up a cart, and takes
 * cash. Completing a sale reuses the proven commerce machinery — it creates a paid,
 * delivered {@link Order} (the single ledger), decrements stock atomically, and settles
 * vendor commission — so an in-store sale shows up everywhere a storefront order does.
 *
 * <p>Correctness guarantees mirror checkout:
 * <ul>
 *   <li><b>Server-side pricing</b> — every line is priced from the current catalog; the
 *       client's prices are ignored.</li>
 *   <li><b>Atomic stock</b> — each line decrements stock; a shortfall rolls the whole
 *       sale back (no partial sales).</li>
 *   <li><b>Idempotent</b> — a re-submitted {@code clientSaleId} returns the existing sale
 *       instead of ringing it up twice (the seam offline sync builds on).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PosService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CommissionService commissionService;
    private final PosSaleRepository posSaleRepository;

    /** Active catalog rows for the terminal grid; an empty query lists everything active. */
    public PageResponse<PosProductResponse> searchProducts(UUID tenantId, String query, Pageable pageable) {
        Page<Product> page = StringUtils.hasText(query)
                ? productRepository.searchActive(tenantId, query.trim(), pageable)
                : productRepository.findByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE, pageable);
        return new PageResponse<>(page.map(PosProductResponse::from));
    }

    public PageResponse<PosSaleResponse> listSales(UUID tenantId, Pageable pageable) {
        String currency = currencyOf(tenantId);
        return new PageResponse<>(posSaleRepository.findByTenantId(tenantId, pageable)
                .map(sale -> PosSaleResponse.from(sale, currency)));
    }

    @Transactional
    public PosSaleResponse completeSale(UUID tenantId, UUID cashierId, CompleteSaleRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Store", tenantId));

        // Idempotency: a re-submitted sale id returns the sale already rung up for it.
        Optional<PosSale> existing = posSaleRepository.findByTenantIdAndClientSaleId(tenantId, request.getClientSaleId());
        if (existing.isPresent()) {
            return PosSaleResponse.from(existing.get(), tenant.getCurrency());
        }

        // Price and stock every line before creating the order; a failure rolls it all back.
        List<OrderItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleLineRequest line : request.getItems()) {
            Product product = productRepository.findByIdAndTenantId(line.getProductId(), tenantId)
                    .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessException("A product in the sale is not available"));
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            stockService.reserve(tenantId, product.getId(), line.getVariantId(), line.getQuantity());

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setVariantId(line.getVariantId());
            item.setVendorId(product.getVendorId());
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setTotal(lineTotal);
            lines.add(item);
        }

        BigDecimal total = subtotal; // no tax/shipping on an in-store cash sale
        if (request.getAmountTendered().compareTo(total) < 0) {
            throw new BusinessException("Cash tendered (" + request.getAmountTendered()
                    + ") is less than the total (" + total + ")");
        }
        BigDecimal change = request.getAmountTendered().subtract(total);

        // An in-store sale completes immediately: a paid, delivered order is the ledger entry.
        Order order = new Order();
        order.setTenantId(tenantId);
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerName("Walk-in customer");
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setSubtotal(subtotal);
        order.setShippingAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotal(total);
        order.setCurrency(tenant.getCurrency());
        order.setIdempotencyKey(request.getClientSaleId());
        Order savedOrder = orderRepository.save(order);

        lines.forEach(item -> item.setOrderId(savedOrder.getId()));
        orderItemRepository.saveAll(lines);

        // Cash is in: earn commission for every vendor line of the sale (idempotent).
        commissionService.settleOrder(tenantId, savedOrder.getId(), savedOrder.getOrderNumber(), vendorLines(lines));

        int itemCount = request.getItems().stream().mapToInt(SaleLineRequest::getQuantity).sum();
        PosSale sale = posSaleRepository.save(new PosSale(tenantId, savedOrder.getId(), savedOrder.getOrderNumber(),
                cashierId, request.getClientSaleId(), subtotal, total,
                request.getAmountTendered(), change, itemCount));
        return PosSaleResponse.from(sale, tenant.getCurrency());
    }

    /** Maps the sale's vendor lines (house products excluded) into commission lines. */
    private static List<CommissionLine> vendorLines(List<OrderItem> items) {
        return items.stream()
                .filter(item -> item.getVendorId() != null)
                .map(item -> new CommissionLine(item.getVendorId(), item.getId(), item.getTotal()))
                .toList();
    }

    private String currencyOf(UUID tenantId) {
        return tenantRepository.findById(tenantId).map(Tenant::getCurrency).orElse("USD");
    }

    /** A unique, channel-tagged receipt number. "POS-" distinguishes in-store sales. */
    private String generateOrderNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "POS-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()
                    + "-" + (100 + RANDOM.nextInt(900));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("Could not generate a receipt number, please retry");
    }
}
