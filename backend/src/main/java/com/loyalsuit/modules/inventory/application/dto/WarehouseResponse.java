package com.loyalsuit.modules.inventory.application.dto;

import com.loyalsuit.modules.inventory.domain.Warehouse;

import java.time.Instant;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String name,
        String address,
        boolean isDefault,
        boolean active,
        Instant createdAt) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.isDefault(),
                warehouse.isActive(),
                warehouse.getCreatedAt());
    }
}
