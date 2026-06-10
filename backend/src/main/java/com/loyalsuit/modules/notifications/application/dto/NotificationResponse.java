package com.loyalsuit.modules.notifications.application.dto;

import com.loyalsuit.modules.notifications.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String body,
        String link,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType().name(), n.getTitle(), n.getBody(), n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
