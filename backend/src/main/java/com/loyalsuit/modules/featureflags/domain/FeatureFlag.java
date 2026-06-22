package com.loyalsuit.modules.featureflags.domain;

import com.loyalsuit.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A global platform feature flag (on/off switch). */
@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@NoArgsConstructor
public class FeatureFlag extends AuditableEntity {

    @Column(name = "flag_key", nullable = false, unique = true)
    private String flagKey;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled = false;
}
