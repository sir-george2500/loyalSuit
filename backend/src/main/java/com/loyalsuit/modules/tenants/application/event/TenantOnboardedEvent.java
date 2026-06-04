package com.loyalsuit.modules.tenants.application.event;

import java.util.UUID;

/**
 * Raised when a tenant finishes the setup wizard. Consumed after the onboarding
 * transaction commits, so listeners (e.g. welcome email) never fire on rollback.
 */
public record TenantOnboardedEvent(
        UUID tenantId,
        String businessName,
        String adminEmail,
        String currency) {
}
