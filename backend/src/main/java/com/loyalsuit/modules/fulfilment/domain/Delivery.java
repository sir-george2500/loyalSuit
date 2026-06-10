package com.loyalsuit.modules.fulfilment.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import com.loyalsuit.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One delivery per order — the courier-side record layered on the orders ledger (mirroring
 * how a POS sale layers on an order). It carries the assigned agent and walks a guarded
 * status lifecycle ({@link DeliveryStatus}); {@code version} guards concurrent transitions
 * so two updates can't both apply. The order number, customer name, and agent name are
 * snapshotted for display without cross-module reads.
 */
@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class Delivery extends TenantScopedEntity {

    @Column(name = "order_id", nullable = false, unique = true, updatable = false)
    private UUID orderId;

    @Column(name = "order_number", nullable = false, updatable = false)
    private String orderNumber;

    @Column(name = "customer_name")
    private String customerName;

    /** The assigned agent (DeliveryAgent id), null while still PENDING. */
    @Column(name = "agent_id")
    private UUID agentId;

    /** The agent's display name at assignment time (snapshot). */
    @Column(name = "agent_name")
    private String agentName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Proof of delivery, captured at completion: who took it, an optional note, and an
     *  optional signature/photo URL. Null until the delivery is marked delivered. */
    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "pod_note", length = 500)
    private String podNote;

    @Column(name = "proof_image_url", length = 500)
    private String proofImageUrl;

    @Version
    @Column(nullable = false)
    private long version;

    public Delivery(UUID tenantId, UUID orderId, String orderNumber, String customerName) {
        this.setTenantId(tenantId);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerName = customerName;
    }

    /** (Re)assign a courier. Only allowed before pickup; moves a PENDING delivery to ASSIGNED. */
    public void assignTo(UUID agentId, String agentName) {
        if (status != DeliveryStatus.PENDING && status != DeliveryStatus.ASSIGNED) {
            throw new BusinessException("A delivery can only be assigned before it is picked up");
        }
        this.agentId = agentId;
        this.agentName = agentName;
        this.status = DeliveryStatus.ASSIGNED;
        this.assignedAt = Instant.now();
    }

    /** Complete the delivery with proof: marks it DELIVERED (only valid from IN_TRANSIT)
     *  and records who received it. */
    public void markDelivered(String recipientName, String note, String proofImageUrl) {
        advanceTo(DeliveryStatus.DELIVERED, null);
        this.recipientName = recipientName;
        this.podNote = note;
        this.proofImageUrl = proofImageUrl;
    }

    /** Advance along the lifecycle; {@code reason} is required only for FAILED. */
    public void advanceTo(DeliveryStatus target, String reason) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessException("A delivery can't move from " + status + " to " + target);
        }
        this.status = target;
        switch (target) {
            case PICKED_UP -> this.pickedUpAt = Instant.now();
            case DELIVERED -> this.deliveredAt = Instant.now();
            case FAILED -> {
                this.failedAt = Instant.now();
                this.failureReason = reason;
            }
            default -> { /* IN_TRANSIT carries no dedicated timestamp */ }
        }
    }
}
