package com.loyalsuit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Identity of the flagship store that <em>is</em> the public marketplace. LoyalSuit runs a single
 * canonical storefront; new sellers join it as vendors rather than spinning up isolated stores.
 * The public storefront, search, cart and checkout all resolve this slug.
 */
@Component
@ConfigurationProperties(prefix = "app.marketplace")
@Getter
@Setter
public class MarketplaceProperties {

    /** Slug of the canonical marketplace store. The public site always resolves this tenant. */
    private String slug = "loyalsuit";

    /** Display name used when first seeding the flagship store. */
    private String name = "Loyal Spare Parts";

    /** Default currency for the flagship store (Rwandan Franc — the brand is Kigali-based). */
    private String currency = "RWF";

    /**
     * Optional operator admin, seeded once at startup so vendor applications can be approved.
     * Only created when BOTH email and password are set — there is never a default credential.
     */
    private String adminEmail = "";
    private String adminPassword = "";
}
