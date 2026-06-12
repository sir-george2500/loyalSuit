package com.loyalsuit.modules.storefront.application.dto;

import com.loyalsuit.modules.tenants.domain.Tenant;

/**
 * One card on the public marketplace index. Carries just enough to render a store tile and link to
 * {@code /store/{slug}} — no internal/operational fields. {@code productCount} is the number of
 * published (ACTIVE) products, so the UI can show how stocked a store is.
 */
public record StoreSummary(
        String name,
        String slug,
        String currency,
        String country,
        String logoUrl,
        long productCount) {

    public static StoreSummary from(Tenant tenant, long productCount) {
        return new StoreSummary(
                tenant.getName(),
                tenant.getSlug(),
                tenant.getCurrency(),
                tenant.getCountry(),
                tenant.getLogoUrl(),
                productCount);
    }
}
