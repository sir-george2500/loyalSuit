package com.loyalsuit.modules.notifications.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * An in-app notification for one user: a titled message, optionally deep-linking to the
 * thing it's about, with a read flag for the inbox. Created by {@code NotificationService}
 * when a notable event happens (an order is placed, a payout is paid, …).
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends TenantScopedEntity {

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Column(nullable = false, updatable = false)
    private String title;

    @Column(length = 1000, updatable = false)
    private String body;

    /** An in-app path the notification links to (e.g. /admin/orders), if any. */
    @Column(updatable = false)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    public Notification(UUID tenantId, UUID recipientId, NotificationType type,
                        String title, String body, String link) {
        this.setTenantId(tenantId);
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
    }

    public void markRead() {
        if (!read) {
            this.read = true;
            this.readAt = Instant.now();
        }
    }
}
