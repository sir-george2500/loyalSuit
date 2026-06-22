package com.loyalsuit.modules.orders.application;

import com.loyalsuit.modules.orders.application.dto.PurchasedItem;
import com.loyalsuit.modules.orders.domain.port.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Read-only collaboration API over orders: "did this customer receive this product?".
 * A purchase counts only once the order is DELIVERED, so a review can't be written for
 * something that hasn't actually arrived. Returns the selling vendor snapshot so callers
 * stay free of order-line internals.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseQueryService {

    private final OrderItemRepository orderItemRepository;

    public Optional<PurchasedItem> findDeliveredPurchase(UUID tenantId, UUID customerId, UUID productId) {
        return orderItemRepository.findDeliveredItems(tenantId, customerId, productId).stream()
                .findFirst()
                .map(item -> new PurchasedItem(item.getProductId(), item.getVendorId()));
    }
}
