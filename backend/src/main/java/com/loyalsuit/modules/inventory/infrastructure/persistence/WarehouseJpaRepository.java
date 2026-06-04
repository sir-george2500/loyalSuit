package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface WarehouseJpaRepository extends JpaRepository<Warehouse, UUID> {
    boolean existsByTenantId(UUID tenantId);
}
