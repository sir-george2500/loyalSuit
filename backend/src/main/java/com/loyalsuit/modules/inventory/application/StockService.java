package com.loyalsuit.modules.inventory.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.catalog.application.ProductVariantService;
import com.loyalsuit.modules.inventory.application.dto.SetStockRequest;
import com.loyalsuit.modules.inventory.application.dto.StockResponse;
import com.loyalsuit.modules.inventory.domain.Stock;
import com.loyalsuit.modules.inventory.domain.port.StockRepository;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Tracks on-hand stock per product/variant/warehouse.
 *
 * <p>Concurrency: absolute {@code setLevel} writes use JPA optimistic locking
 * (the {@code @Version} on {@link Stock}); concurrent quantity {@code adjust}ments
 * go through an atomic conditional UPDATE in the repository, which is lost-update
 * safe and cannot drive stock negative.
 *
 * <p>Tenant safety: the warehouse is validated against this module's repository and
 * the product/variant against the catalog via {@link ProductVariantService}, so a
 * stock row can never reference another tenant's product, variant, or warehouse.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductVariantService productVariantService;

    public List<StockResponse> listForProduct(UUID productId, UUID tenantId) {
        productVariantService.assertProductAndVariant(productId, null, tenantId);
        return stockRepository.findByProductIdAndTenantId(productId, tenantId).stream()
                .map(StockResponse::from)
                .toList();
    }

    public List<StockResponse> lowStock(UUID tenantId) {
        return stockRepository.findLowStock(tenantId).stream()
                .map(StockResponse::from)
                .toList();
    }

    /** Sets the absolute quantity and threshold for a product/variant/warehouse row. */
    @Transactional
    public StockResponse setLevel(SetStockRequest request, UUID tenantId) {
        productVariantService.assertProductAndVariant(request.getProductId(), request.getVariantId(), tenantId);
        requireWarehouse(request.getWarehouseId(), tenantId);

        Stock stock = stockRepository
                .findExisting(request.getProductId(), request.getVariantId(), request.getWarehouseId(), tenantId)
                .orElseGet(() -> new Stock(
                        tenantId, request.getProductId(), request.getVariantId(), request.getWarehouseId()));

        stock.setQuantity(request.getQuantity());
        stock.setLowStockThreshold(request.getLowStockThreshold());
        return StockResponse.from(stockRepository.save(stock));
    }

    /**
     * Applies a signed delta atomically. A delta that would make the quantity
     * negative is rejected (insufficient stock); the row must already exist.
     */
    @Transactional
    public StockResponse adjust(UUID stockId, int delta, UUID tenantId) {
        // Confirm the row is the caller's before touching it.
        stockRepository.findByIdAndTenantId(stockId, tenantId)
                .orElseThrow(() -> new NotFoundException("Stock", stockId));

        int updated = stockRepository.applyDelta(stockId, tenantId, delta);
        if (updated == 0) {
            throw new BusinessException("Not enough stock for that adjustment");
        }
        // Re-read after the atomic update (the persistence context was cleared).
        return stockRepository.findByIdAndTenantId(stockId, tenantId)
                .map(StockResponse::from)
                .orElseThrow(() -> new NotFoundException("Stock", stockId));
    }

    private void requireWarehouse(UUID warehouseId, UUID tenantId) {
        warehouseRepository.findByIdAndTenantId(warehouseId, tenantId)
                .orElseThrow(() -> new BusinessException("Warehouse does not exist in this store"));
    }
}
