package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WarehouseJpaRepository extends JpaRepository<Warehouse, UUID> {
    Optional<Warehouse> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Warehouse> findByTenantIdOrderByName(UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
    int countByTenantId(UUID tenantId);

    // JPQL by field name avoids the boolean-property naming pitfall for `isDefault`.
    @Query("SELECT w FROM Warehouse w WHERE w.tenantId = :tenantId AND w.isDefault = true")
    Optional<Warehouse> findDefault(@Param("tenantId") UUID tenantId);
}
