package com.loyalsuit.modules.inventory.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * On-hand quantity of a product (optionally a specific variant) at one warehouse.
 * The {@code (product_id, variant_id, warehouse_id)} triple is unique per row.
 * {@code version} drives optimistic locking on absolute writes; delta adjustments
 * go through an atomic conditional UPDATE in the repository.
 */
@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock extends TenantScopedEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Version
    @Column(nullable = false)
    private long version;

    public Stock(UUID tenantId, UUID productId, UUID variantId, UUID warehouseId) {
        this.setTenantId(tenantId);
        this.productId = productId;
        this.variantId = variantId;
        this.warehouseId = warehouseId;
    }

    public boolean isLowStock() {
        return quantity <= lowStockThreshold;
    }
}
