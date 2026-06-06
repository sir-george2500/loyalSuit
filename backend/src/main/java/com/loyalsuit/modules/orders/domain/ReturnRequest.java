package com.loyalsuit.modules.orders.domain;

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

/** A customer's request to return a delivered order, pending admin review. */
@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReturnRequest extends TenantScopedEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** Denormalized for display so listing returns needs no order lookups. */
    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    public ReturnRequest(UUID tenantId, UUID orderId, String orderNumber, String reason, String customerEmail) {
        this.setTenantId(tenantId);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.reason = reason;
        this.customerEmail = customerEmail;
    }
}
