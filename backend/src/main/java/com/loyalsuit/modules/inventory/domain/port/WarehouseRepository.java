package com.loyalsuit.modules.inventory.domain.port;

import com.loyalsuit.modules.inventory.domain.Warehouse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Warehouse> findByTenantIdOrderByName(UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
    int countByTenantId(UUID tenantId);
    void deleteById(UUID id);
}
