package com.loyalsuit.modules.platform.application.dto;

import com.loyalsuit.modules.tenants.domain.SubscriptionPlan;

import java.time.Instant;
import java.util.UUID;

/** A tenant as seen by the platform administrator. */
public record TenantAdminResponse(
        UUID id,
        String name,
        String slug,
        SubscriptionPlan subscriptionPlan,
        boolean active,
        String currency,
        String country,
        boolean onboarded,
        Instant createdAt) {}
