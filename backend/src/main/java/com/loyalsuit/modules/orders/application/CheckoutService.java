package com.loyalsuit.modules.orders.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.cart.application.CartService;
import com.loyalsuit.modules.cart.application.dto.CartItemView;
import com.loyalsuit.modules.cart.application.dto.CartView;
import com.loyalsuit.modules.catalog.domain.Product;
import com.loyalsuit.modules.catalog.domain.port.ProductRepository;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.orders.application.dto.CheckoutRequest;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.modules.orders.application.port.ResolvedShippingZone;
import com.loyalsuit.modules.orders.application.port.ShippingZoneResolver;
import com.loyalsuit.modules.orders.domain.Order;
import com.loyalsuit.modules.orders.domain.OrderItem;
import com.loyalsuit.modules.orders.domain.OrderStatus;
import com.loyalsuit.modules.orders.domain.PaymentMethod;
import com.loyalsuit.modules.orders.domain.PaymentStatus;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import com.loyalsuit.modules.orders.domain.port.OrderRepository;
import com.loyalsuit.modules.tenants.domain.Tenant;
import com.loyalsuit.modules.tenants.domain.port.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Places a cash order from a cart. Correctness guarantees:
 * <ul>
 *   <li><b>Server-side totals</b> — line prices come from {@link CartService}'s
 *       recompute (current catalog), never from the client.</li>
 *   <li><b>Atomic stock reservation</b> — each line decrements stock atomically; if
 *       any line is short, the whole checkout rolls back (no partial orders).</li>
 *   <li><b>Idempotent</b> — a repeated idempotency key returns the existing order
 *       instead of creating a duplicate.</li>
 * </ul>
 * Payment is cash: the order is created {@code CASH} / {@code UNPAID}.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final CartService cartService;
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ShippingZoneResolver shippingZoneResolver;

    @Transactional
    public OrderResponse checkout(String storeSlug, String cartToken, CheckoutRequest request,
                                  String idempotencyKey, UUID authUserId, UUID authTenantId) {
        Tenant tenant = requireStore(storeSlug);

        // Idempotency: a re-submitted key returns the order already created for it.
        if (StringUtils.hasText(idempotencyKey)) {
            Optional<Order> existing = orderRepository.findByTenantIdAndIdempotencyKey(tenant.getId(), idempotencyKey);
            if (existing.isPresent()) {
                return OrderResponse.from(existing.get(), orderItemRepository.findByOrderId(existing.get().getId()));
            }
        }

        CartView cart = cartService.view(storeSlug, cartToken);
        if (cart.items().isEmpty()) {
            throw new BusinessException("Your cart is empty");
        }

        // Shipping is priced from the chosen delivery zone (its fee), 0 if none was selected.
        Map<String, Object> address = buildAddress(request);
        BigDecimal shipping = BigDecimal.ZERO;
        if (request.getDeliveryZoneId() != null) {
            ResolvedShippingZone zone = shippingZoneResolver
                    .resolveActive(tenant.getId(), request.getDeliveryZoneId())
                    .orElseThrow(() -> new BusinessException("The selected delivery zone is unavailable"));
            shipping = zone.fee();
            address.put("pickupPoint", zone.pickupPointName());
            address.put("deliveryZone", zone.zoneName());
        }

        // Reserve stock for every line before creating the order. A shortfall throws,
        // rolling back any decrements already applied in this transaction.
        for (CartItemView line : cart.items()) {
            stockService.reserve(tenant.getId(), line.productId(), line.variantId(), line.quantity());
        }

        Order order = new Order();
        order.setTenantId(tenant.getId());
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerId(resolveCustomer(tenant, authUserId, authTenantId));
        order.setCustomerName(request.getCustomerName().trim());
        order.setCustomerEmail(trimToNull(request.getCustomerEmail()));
        order.setCustomerPhone(trimToNull(request.getCustomerPhone()));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setSubtotal(cart.subtotal());
        order.setShippingAmount(shipping);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotal(cart.subtotal().add(shipping));
        order.setCurrency(cart.currency());
        order.setShippingAddress(address);
        order.setNotes(trimToNull(request.getNotes()));
        order.setIdempotencyKey(StringUtils.hasText(idempotencyKey) ? idempotencyKey : null);
        Order saved = orderRepository.save(order);

        List<OrderItem> items = new ArrayList<>();
        for (CartItemView line : cart.items()) {
            OrderItem item = new OrderItem();
            item.setOrderId(saved.getId());
            item.setProductId(line.productId());
            item.setVariantId(line.variantId());
            item.setVendorId(vendorIdOf(line.productId(), tenant.getId()));
            item.setQuantity(line.quantity());
            item.setUnitPrice(line.unitPrice());
            item.setTotal(line.lineTotal());
            items.add(item);
        }
        orderItemRepository.saveAll(items);

        cartService.clear(storeSlug, cartToken);
        return OrderResponse.from(saved, items);
    }

    /** The selling vendor for a product (their user id), snapshotted onto the order line. */
    private UUID vendorIdOf(UUID productId, UUID tenantId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .map(Product::getVendorId)
                .orElse(null);
    }

    /** Link an authenticated customer of this store; otherwise the order is a guest order. */
    private UUID resolveCustomer(Tenant tenant, UUID authUserId, UUID authTenantId) {
        return authUserId != null && tenant.getId().equals(authTenantId) ? authUserId : null;
    }

    private String generateOrderNumber() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "ORD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase()
                    + "-" + (100 + RANDOM.nextInt(900));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("Could not generate an order number, please retry");
    }

    private static Map<String, Object> buildAddress(CheckoutRequest request) {
        Map<String, Object> address = new LinkedHashMap<>();
        putIfText(address, "line1", request.getAddressLine1());
        putIfText(address, "line2", request.getAddressLine2());
        putIfText(address, "city", request.getCity());
        putIfText(address, "state", request.getState());
        putIfText(address, "postalCode", request.getPostalCode());
        putIfText(address, "country", request.getCountry());
        return address;
    }

    private static void putIfText(Map<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value.trim());
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Tenant requireStore(String storeSlug) {
        return tenantRepository.findBySlug(storeSlug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Store", storeSlug));
    }
}
