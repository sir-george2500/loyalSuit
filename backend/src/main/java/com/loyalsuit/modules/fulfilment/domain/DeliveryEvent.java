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
 * One entry in a delivery's status timeline: the status it entered, who moved it, and an
 * optional note. {@code occurredAt} is the row's {@code createdAt}. Append-only — the
 * timeline is the audit trail of how a delivery progressed.
 */
@Entity
@Table(name = "delivery_events")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryEvent extends TenantScopedEntity {

    @Column(name = "delivery_id", nullable = false, updatable = false)
    private UUID deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private DeliveryStatus status;

    @Column(length = 500, updatable = false)
    private String note;

    /** The user who caused the transition (admin/dispatcher or the agent). */
    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    public DeliveryEvent(UUID tenantId, UUID deliveryId, DeliveryStatus status, String note, UUID actorId) {
        this.setTenantId(tenantId);
        this.deliveryId = deliveryId;
        this.status = status;
        this.note = note;
        this.actorId = actorId;
    }
}
