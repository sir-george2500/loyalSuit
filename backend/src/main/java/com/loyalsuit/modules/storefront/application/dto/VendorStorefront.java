package com.loyalsuit.modules.storefront.application.dto;

import com.loyalsuit.modules.marketplace.domain.Vendor;

/**
 * Public profile of a vendor's storefront within the LoyalSuit marketplace. Only customer-facing
 * fields — no commission rate, status, or owner identity.
 */
public record VendorStorefront(
        String storeName,
        String slug,
        String description,
        String logoUrl) {

    public static VendorStorefront from(Vendor vendor) {
        return new VendorStorefront(
                vendor.getStoreName(),
                vendor.getSlug(),
                vendor.getDescription(),
                vendor.getLogoUrl());
    }
}
