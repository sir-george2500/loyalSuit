package com.loyalsuit.modules.notifications.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.notifications.application.dto.NotificationResponse;
import com.loyalsuit.modules.notifications.domain.Notification;
import com.loyalsuit.modules.notifications.domain.NotificationType;
import com.loyalsuit.modules.notifications.domain.port.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * In-app notifications. Other modules call {@link #notify} when something notable happens
 * (an order placed, a payout paid); each recipient reads their own inbox here. Tenant- and
 * recipient-scoped throughout. (Email delivery can layer on top of {@code notify} later.)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** Create an in-app notification for a user. A null recipient is a no-op (e.g. a guest). */
    @Transactional
    public void notify(UUID tenantId, UUID recipientId, NotificationType type,
                       String title, String body, String link) {
        if (recipientId == null) {
            return;
        }
        notificationRepository.save(new Notification(tenantId, recipientId, type, title, body, link));
    }

    public PageResponse<NotificationResponse> list(UUID tenantId, UUID recipientId, Pageable pageable) {
        return new PageResponse<>(notificationRepository
                .findByTenantIdAndRecipientId(tenantId, recipientId, pageable)
                .map(NotificationResponse::from));
    }

    public long unreadCount(UUID tenantId, UUID recipientId) {
        return notificationRepository.countUnread(tenantId, recipientId);
    }

    @Transactional
    public void markRead(UUID id, UUID tenantId, UUID recipientId) {
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .filter(n -> n.getRecipientId().equals(recipientId))
                .orElseThrow(() -> new NotFoundException("Notification", id));
        notification.markRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(UUID tenantId, UUID recipientId) {
        notificationRepository.markAllRead(tenantId, recipientId);
    }
}
