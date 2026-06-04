package com.loyalsuit.modules.inventory.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Warehouse(UUID tenantId, String name, String address, boolean isDefault) {
        this.setTenantId(tenantId);
        this.name = name;
        this.address = address;
        this.isDefault = isDefault;
    }
}
