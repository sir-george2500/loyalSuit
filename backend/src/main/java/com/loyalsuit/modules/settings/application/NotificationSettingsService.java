package com.loyalsuit.modules.settings.application;

import com.loyalsuit.modules.settings.application.dto.NotificationPreferencesDto;
import com.loyalsuit.modules.settings.domain.NotificationPreferences;
import com.loyalsuit.modules.settings.domain.port.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reads and updates a tenant's notification toggles. The row is created lazily: a tenant
 * that has never saved preferences sees the entity defaults, and the first save persists them.
 */
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationPreferencesRepository repository;

    @Transactional(readOnly = true)
    public NotificationPreferencesDto get(UUID tenantId) {
        return repository.findByTenantId(tenantId)
                .map(NotificationSettingsService::toDto)
                .orElseGet(() -> toDto(new NotificationPreferences(tenantId)));
    }

    @Transactional
    public NotificationPreferencesDto update(UUID tenantId, NotificationPreferencesDto request) {
        NotificationPreferences prefs = repository.findByTenantId(tenantId)
                .orElseGet(() -> new NotificationPreferences(tenantId));
        prefs.setOrderConfirmationEmail(request.orderConfirmationEmail());
        prefs.setOrderStatusEmail(request.orderStatusEmail());
        prefs.setLowStockAlert(request.lowStockAlert());
        prefs.setNewReviewAlert(request.newReviewAlert());
        prefs.setPayoutAlert(request.payoutAlert());
        prefs.setMarketingEmail(request.marketingEmail());
        return toDto(repository.save(prefs));
    }

    private static NotificationPreferencesDto toDto(NotificationPreferences p) {
        return new NotificationPreferencesDto(
                p.isOrderConfirmationEmail(), p.isOrderStatusEmail(), p.isLowStockAlert(),
                p.isNewReviewAlert(), p.isPayoutAlert(), p.isMarketingEmail());
    }
}
