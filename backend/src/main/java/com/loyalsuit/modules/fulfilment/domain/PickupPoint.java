package com.loyalsuit.modules.fulfilment.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A location a store dispatches/picks orders from. Each pickup point owns its own set of
 * {@link DeliveryZone}s, so two points can charge different areas differently (a downtown
 * branch and a suburban one cover different zones). An inactive point keeps its zones and
 * history but isn't offered at checkout.
 */
@Entity
@Table(name = "pickup_points")
@Getter
@Setter
@NoArgsConstructor
public class PickupPoint extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public PickupPoint(UUID tenantId, String name) {
        this.setTenantId(tenantId);
        this.name = name;
    }
}
