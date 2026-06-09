package com.loyalsuit.modules.fulfilment.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A delivery agent's roster profile: the courier identity an admin onboards and later
 * assigns orders to. It links a {@code DELIVERY_AGENT} user ({@code userId}, unique — one
 * profile per user) to their contact and vehicle; the agent's name/email live on that user.
 * An inactive agent stays on the roster (and keeps their delivery history) but isn't
 * assignable.
 */
@Entity
@Table(name = "delivery_agents")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryAgent extends TenantScopedEntity {

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public DeliveryAgent(UUID tenantId, UUID userId, String phone, VehicleType vehicleType) {
        this.setTenantId(tenantId);
        this.userId = userId;
        this.phone = phone;
        this.vehicleType = vehicleType;
    }
}
