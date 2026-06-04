package com.loyalsuit.modules.inventory.domain.port;

import com.loyalsuit.modules.inventory.domain.Warehouse;

import java.util.UUID;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    boolean existsByTenantId(UUID tenantId);
}
