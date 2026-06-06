package com.loyalsuit.modules.inventory.infrastructure.persistence;

import com.loyalsuit.modules.inventory.domain.Warehouse;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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
    public Optional<Warehouse> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Warehouse> findByTenantIdOrderByName(UUID tenantId) {
        return jpa.findByTenantIdOrderByName(tenantId);
    }

    @Override
    public boolean existsByTenantId(UUID tenantId) {
        return jpa.existsByTenantId(tenantId);
    }

    @Override
    public int countByTenantId(UUID tenantId) {
        return jpa.countByTenantId(tenantId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
