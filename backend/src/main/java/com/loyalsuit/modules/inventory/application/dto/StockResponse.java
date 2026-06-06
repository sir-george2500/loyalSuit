package com.loyalsuit.modules.inventory.application.dto;

import com.loyalsuit.modules.inventory.domain.Stock;

import java.util.UUID;

public record StockResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        UUID warehouseId,
        int quantity,
        int lowStockThreshold,
        boolean lowStock) {

    public static StockResponse from(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getProductId(),
                stock.getVariantId(),
                stock.getWarehouseId(),
                stock.getQuantity(),
                stock.getLowStockThreshold(),
                stock.isLowStock());
    }
}
