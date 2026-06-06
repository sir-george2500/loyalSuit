package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WarehouseJpaRepository extends JpaRepository<Warehouse, UUID> {
    Optional<Warehouse> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Warehouse> findByTenantIdOrderByName(UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
    int countByTenantId(UUID tenantId);
}
