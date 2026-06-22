package com.loyalsuit.modules.plans.domain;

import com.loyalsuit.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A platform subscription plan definition (the catalogue tenants are billed against).
 * Global, not tenant-scoped — managed by the platform owner.
 */
@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
public class PlatformPlan extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 20)
    private BillingInterval billingInterval = BillingInterval.MONTHLY;

    @Column(name = "max_products")
    private Integer maxProducts;

    @Column(name = "max_staff")
    private Integer maxStaff;

    @Column(nullable = false)
    private boolean active = true;
}
