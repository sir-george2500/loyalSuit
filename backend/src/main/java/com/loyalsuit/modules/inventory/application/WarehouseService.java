package com.loyalsuit.modules.inventory.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.inventory.application.dto.WarehouseRequest;
import com.loyalsuit.modules.inventory.application.dto.WarehouseResponse;
import com.loyalsuit.modules.inventory.domain.Warehouse;
import com.loyalsuit.modules.inventory.domain.port.StockRepository;
import com.loyalsuit.modules.inventory.domain.port.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Warehouse management for a tenant. Maintains the invariant that a tenant has at
 * most one default warehouse, and never lets a tenant delete its last one or the
 * current default (which would leave the store without a place to hold stock).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;

    public List<WarehouseResponse> list(UUID tenantId) {
        return warehouseRepository.findByTenantIdOrderByName(tenantId).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    public WarehouseResponse getById(UUID id, UUID tenantId) {
        return WarehouseResponse.from(load(id, tenantId));
    }

    @Transactional
    public WarehouseResponse create(WarehouseRequest request, UUID tenantId) {
        // The very first warehouse is always the default, regardless of the request.
        boolean makeDefault = request.isDefault() || !warehouseRepository.existsByTenantId(tenantId);

        Warehouse warehouse = new Warehouse(
                tenantId, request.getName().trim(), trimToNull(request.getAddress()), makeDefault);
        if (makeDefault) {
            clearOtherDefaults(tenantId, null);
        }
        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    public WarehouseResponse update(UUID id, WarehouseRequest request, UUID tenantId) {
        Warehouse warehouse = load(id, tenantId);

        warehouse.setName(request.getName().trim());
        warehouse.setAddress(trimToNull(request.getAddress()));
        if (request.isDefault() && !warehouse.isDefault()) {
            clearOtherDefaults(tenantId, id);
            warehouse.setDefault(true);
        }
        return WarehouseResponse.from(warehouseRepository.save(warehouse));
    }

    @Transactional
    public void delete(UUID id, UUID tenantId) {
        Warehouse warehouse = load(id, tenantId);
        if (warehouse.isDefault()) {
            throw new ConflictException("Set another warehouse as default before deleting this one");
        }
        if (warehouseRepository.countByTenantId(tenantId) <= 1) {
            throw new ConflictException("A store must keep at least one warehouse");
        }
        if (stockRepository.existsByWarehouseId(id)) {
            throw new ConflictException("Move or clear this warehouse's stock before deleting it");
        }
        warehouseRepository.deleteById(id);
    }

    private Warehouse load(UUID id, UUID tenantId) {
        return warehouseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Warehouse", id));
    }

    /** Unset the default flag on every other warehouse so exactly one stays default. */
    private void clearOtherDefaults(UUID tenantId, UUID exceptId) {
        for (Warehouse other : warehouseRepository.findByTenantIdOrderByName(tenantId)) {
            if (other.isDefault() && !other.getId().equals(exceptId)) {
                other.setDefault(false);
                warehouseRepository.save(other);
            }
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
