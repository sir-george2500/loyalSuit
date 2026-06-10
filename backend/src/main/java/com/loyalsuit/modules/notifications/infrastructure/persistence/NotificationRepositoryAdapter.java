package com.loyalsuit.modules.notifications.infrastructure.persistence;

import com.loyalsuit.modules.notifications.domain.Notification;
import com.loyalsuit.modules.notifications.domain.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;

    @Override
    public Notification save(Notification notification) {
        return jpa.save(notification);
    }

    @Override
    public Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Page<Notification> findByTenantIdAndRecipientId(UUID tenantId, UUID recipientId, Pageable pageable) {
        return jpa.findByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, recipientId, pageable);
    }

    @Override
    public long countUnread(UUID tenantId, UUID recipientId) {
        return jpa.countByTenantIdAndRecipientIdAndReadFalse(tenantId, recipientId);
    }

    @Override
    public int markAllRead(UUID tenantId, UUID recipientId) {
        return jpa.markAllRead(tenantId, recipientId, Instant.now());
    }
}
