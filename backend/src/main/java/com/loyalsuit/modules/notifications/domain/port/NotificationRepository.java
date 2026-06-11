package com.loyalsuit.modules.notifications.domain.port;

import com.loyalsuit.modules.notifications.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Notification> findByTenantIdAndRecipientId(UUID tenantId, UUID recipientId, Pageable pageable);
    long countUnread(UUID tenantId, UUID recipientId);
    int markAllRead(UUID tenantId, UUID recipientId);

    /** All of a recipient's notifications (for a personal-data export). */
    List<Notification> findAllByRecipient(UUID tenantId, UUID recipientId);

    /** Delete a recipient's notifications (erasure); returns how many were removed. */
    int deleteByRecipient(UUID tenantId, UUID recipientId);
}
