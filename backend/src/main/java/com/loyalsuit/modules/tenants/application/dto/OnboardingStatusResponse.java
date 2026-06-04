package com.loyalsuit.modules.tenants.application.dto;

import com.loyalsuit.modules.tenants.domain.Tenant;

/**
 * Current onboarding state for the tenant, used to gate the setup wizard. Returns
 * the existing profile so the wizard can pre-fill fields on a partial revisit.
 */
public record OnboardingStatusResponse(
        boolean onboarded,
        String businessName,
        String currency,
        String country,
        String timezone,
        String phone) {

    public static OnboardingStatusResponse from(Tenant tenant) {
        return new OnboardingStatusResponse(
                tenant.isOnboarded(),
                tenant.getName(),
                tenant.getCurrency(),
                tenant.getCountry(),
                tenant.getTimezone(),
                tenant.getPhone());
    }
}
