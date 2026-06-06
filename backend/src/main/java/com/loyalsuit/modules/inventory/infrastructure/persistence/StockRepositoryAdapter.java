package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Stock;
import com.loyalsuit.modules.inventory.domain.port.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockRepositoryAdapter implements StockRepository {

    private final StockJpaRepository jpa;

    @Override
    public Stock save(Stock stock) {
        return jpa.save(stock);
    }

    @Override
    public Optional<Stock> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Stock> findByProductIdAndTenantId(UUID productId, UUID tenantId) {
        return jpa.findByProductIdAndTenantId(productId, tenantId);
    }

    @Override
    public List<Stock> findLowStock(UUID tenantId) {
        return jpa.findLowStock(tenantId);
    }

    @Override
    public boolean existsByWarehouseId(UUID warehouseId) {
        return jpa.existsByWarehouseId(warehouseId);
    }

    @Override
    public boolean hasStock(UUID productId, UUID tenantId) {
        return jpa.existsByProductIdAndTenantIdAndQuantityGreaterThan(productId, tenantId, 0);
    }

    @Override
    public Optional<Stock> findExisting(UUID productId, UUID variantId, UUID warehouseId, UUID tenantId) {
        return variantId == null
                ? jpa.findByProductIdAndWarehouseIdAndVariantIdIsNullAndTenantId(productId, warehouseId, tenantId)
                : jpa.findByProductIdAndWarehouseIdAndVariantIdAndTenantId(productId, warehouseId, variantId, tenantId);
    }

    @Override
    public int applyDelta(UUID id, UUID tenantId, int delta) {
        return jpa.applyDelta(id, tenantId, delta);
    }
}
