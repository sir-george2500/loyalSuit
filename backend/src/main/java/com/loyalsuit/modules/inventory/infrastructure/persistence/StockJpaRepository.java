package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface StockJpaRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Stock> findByProductIdAndTenantId(UUID productId, UUID tenantId);

    boolean existsByWarehouseId(UUID warehouseId);

    boolean existsByProductIdAndTenantIdAndQuantityGreaterThan(UUID productId, UUID tenantId, int quantity);

    // Explicit nullable-variant finders avoid fragile ":param IS NULL" JPQL.
    Optional<Stock> findByProductIdAndWarehouseIdAndVariantIdIsNullAndTenantId(
            UUID productId, UUID warehouseId, UUID tenantId);

    Optional<Stock> findByProductIdAndWarehouseIdAndVariantIdAndTenantId(
            UUID productId, UUID warehouseId, UUID variantId, UUID tenantId);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND s.quantity <= s.lowStockThreshold")
    List<Stock> findLowStock(@Param("tenantId") UUID tenantId);

    /**
     * Atomic, lost-update-safe delta. The {@code quantity + :delta >= 0} guard makes
     * negative stock impossible without a read-modify-write race. Bumps version so
     * any concurrent optimistic reader detects the change.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Stock s SET s.quantity = s.quantity + :delta, s.version = s.version + 1
            WHERE s.id = :id AND s.tenantId = :tenantId AND s.quantity + :delta >= 0
            """)
    int applyDelta(@Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("delta") int delta);
}
