package com.loyalsuit.modules.catalog.infrastructure.persistence;

import java.util.UUID;

/** Projection for "how many active products does each tenant have" — one grouped query, no N+1. */
public interface TenantProductCount {
    UUID getTenantId();
    long getTotal();
}
