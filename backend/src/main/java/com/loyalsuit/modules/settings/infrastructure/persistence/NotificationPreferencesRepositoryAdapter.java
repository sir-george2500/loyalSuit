package com.loyalsuit.modules.settings.infrastructure.persistence;

import com.loyalsuit.modules.settings.domain.NotificationPreferences;
import com.loyalsuit.modules.settings.domain.port.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationPreferencesRepositoryAdapter implements NotificationPreferencesRepository {

    private final NotificationPreferencesJpaRepository jpa;

    @Override
    public Optional<NotificationPreferences> findByTenantId(UUID tenantId) {
        return jpa.findByTenantId(tenantId);
    }

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        return jpa.save(preferences);
    }
}
