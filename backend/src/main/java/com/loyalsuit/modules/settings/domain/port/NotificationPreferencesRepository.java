package com.loyalsuit.modules.settings.domain.port;

import com.loyalsuit.modules.settings.domain.NotificationPreferences;

import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferencesRepository {
    Optional<NotificationPreferences> findByTenantId(UUID tenantId);
    NotificationPreferences save(NotificationPreferences preferences);
}
