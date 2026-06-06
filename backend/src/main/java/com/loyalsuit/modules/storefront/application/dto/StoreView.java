package com.loyalsuit.modules.storefront.application.dto;

import com.loyalsuit.modules.tenants.domain.Tenant;

/** Public storefront profile for a tenant. No internal/operational fields. */
public record StoreView(
        String name,
        String slug,
        String currency,
        String logoUrl) {

    public static StoreView from(Tenant tenant) {
        return new StoreView(tenant.getName(), tenant.getSlug(), tenant.getCurrency(), tenant.getLogoUrl());
    }
}
