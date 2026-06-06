package com.loyalsuit.modules.inventory.domain.port;

import com.loyalsuit.modules.inventory.domain.Stock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository {
    Stock save(Stock stock);
    Optional<Stock> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Stock> findByProductIdAndTenantId(UUID productId, UUID tenantId);
    List<Stock> findLowStock(UUID tenantId);
    boolean existsByWarehouseId(UUID warehouseId);
    /** True if any warehouse holds a positive quantity of the product (storefront use). */
    boolean hasStock(UUID productId, UUID tenantId);

    /** Locate the row for an exact product/variant/warehouse triple (variant may be null). */
    Optional<Stock> findExisting(UUID productId, UUID variantId, UUID warehouseId, UUID tenantId);

    /**
     * Atomically apply a signed delta to a row's quantity, refusing to go negative.
     * Returns the number of rows updated (0 means the row was missing or the delta
     * would have driven the quantity below zero).
     */
    int applyDelta(UUID id, UUID tenantId, int delta);
}
