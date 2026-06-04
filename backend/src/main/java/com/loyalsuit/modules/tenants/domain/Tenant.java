package com.loyalsuit.modules.tenants.domain;

import com.loyalsuit.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends AuditableEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String logoUrl;

    @Column
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.BASIC;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(length = 2)
    private String country;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Column
    private String phone;

    /** Timestamp the setup wizard was completed; null until onboarded. */
    @Column(name = "onboarded_at")
    private Instant onboardedAt;

    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public boolean isOnboarded() {
        return onboardedAt != null;
    }
}
