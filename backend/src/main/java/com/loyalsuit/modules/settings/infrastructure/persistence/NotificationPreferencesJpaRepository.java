package com.loyalsuit.modules.settings.infrastructure.persistence;

import com.loyalsuit.modules.settings.domain.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface NotificationPreferencesJpaRepository extends JpaRepository<NotificationPreferences, UUID> {
    Optional<NotificationPreferences> findByTenantId(UUID tenantId);
}
