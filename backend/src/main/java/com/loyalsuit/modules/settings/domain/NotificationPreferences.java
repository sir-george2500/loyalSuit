package com.loyalsuit.modules.settings.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A tenant's notification toggles. Exactly one row per tenant (tenant_id is unique). */
@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
public class NotificationPreferences extends TenantScopedEntity {

    @Column(name = "order_confirmation_email", nullable = false)
    private boolean orderConfirmationEmail = true;

    @Column(name = "order_status_email", nullable = false)
    private boolean orderStatusEmail = true;

    @Column(name = "low_stock_alert", nullable = false)
    private boolean lowStockAlert = true;

    @Column(name = "new_review_alert", nullable = false)
    private boolean newReviewAlert = true;

    @Column(name = "payout_alert", nullable = false)
    private boolean payoutAlert = true;

    @Column(name = "marketing_email", nullable = false)
    private boolean marketingEmail = false;

    public NotificationPreferences(UUID tenantId) {
        setTenantId(tenantId);
    }
}
