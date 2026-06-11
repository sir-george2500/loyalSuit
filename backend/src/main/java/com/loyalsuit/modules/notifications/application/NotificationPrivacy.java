package com.loyalsuit.modules.notifications.application;

import com.loyalsuit.modules.notifications.domain.Notification;
import com.loyalsuit.modules.notifications.domain.port.NotificationRepository;
import com.loyalsuit.modules.privacy.domain.port.PersonalDataContributor;
import com.loyalsuit.modules.privacy.domain.port.PersonalDataEraser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * The notifications module's GDPR hooks: its inbox messages are purely personal, so they are
 * exported in full and deleted outright on erasure.
 */
@Component
@RequiredArgsConstructor
public class NotificationPrivacy implements PersonalDataContributor, PersonalDataEraser {

    private final NotificationRepository notificationRepository;

    @Override
    public String section() {
        return "notifications";
    }

    @Override
    public Object export(UUID userId, UUID tenantId) {
        return notificationRepository.findAllByRecipient(tenantId, userId).stream()
                .map(this::toRow)
                .toList();
    }

    @Override
    public void erase(UUID userId, UUID tenantId) {
        notificationRepository.deleteByRecipient(tenantId, userId);
    }

    private Map<String, Object> toRow(Notification n) {
        return Map.of(
                "type", n.getType().name(),
                "title", n.getTitle(),
                "body", n.getBody() == null ? "" : n.getBody(),
                "read", n.isRead(),
                "createdAt", String.valueOf(n.getCreatedAt()));
    }
}
