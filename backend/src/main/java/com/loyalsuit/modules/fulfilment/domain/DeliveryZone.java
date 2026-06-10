package com.loyalsuit.modules.fulfilment.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A delivery area served by one {@link PickupPoint}, with the fee charged to reach it.
 * Zones belong to a point (not the tenant globally), so each point defines its own areas
 * and prices — the customer picks a point and then one of that point's zones at checkout,
 * and the zone's {@code fee} becomes the order's shipping.
 */
@Entity
@Table(name = "delivery_zones")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryZone extends TenantScopedEntity {

    @Column(name = "pickup_point_id", nullable = false, updatable = false)
    private UUID pickupPointId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public DeliveryZone(UUID tenantId, UUID pickupPointId, String name, BigDecimal fee) {
        this.setTenantId(tenantId);
        this.pickupPointId = pickupPointId;
        this.name = name;
        this.fee = fee;
    }
}
