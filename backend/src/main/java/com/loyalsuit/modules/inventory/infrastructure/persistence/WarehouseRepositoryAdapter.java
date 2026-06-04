package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Warehouse;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WarehouseRepositoryAdapter implements WarehouseRepository {

    private final WarehouseJpaRepository jpa;

    @Override
    public Warehouse save(Warehouse warehouse) {
        return jpa.save(warehouse);
    }

    @Override
    public boolean existsByTenantId(UUID tenantId) {
        return jpa.existsByTenantId(tenantId);
    }
}
