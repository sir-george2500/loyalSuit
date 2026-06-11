package com.loyalsuit.modules.hrm.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A recognition recorded against an employee — shown on their profile. Awards are simple,
 * standalone records (title, category, the date given, who gave it); there's no lifecycle.
 */
@Entity
@Table(name = "awards")
@Getter
@Setter
@NoArgsConstructor
public class Award extends TenantScopedEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AwardCategory category;

    @Column(name = "awarded_on", nullable = false)
    private LocalDate awardedOn;

    @Column(name = "description")
    private String description;

    /** Free-text attribution for who granted it (e.g. "Store Manager"); optional. */
    @Column(name = "awarded_by")
    private String awardedBy;

    public Award(UUID tenantId, UUID employeeId, String title, AwardCategory category, LocalDate awardedOn) {
        this.setTenantId(tenantId);
        this.employeeId = employeeId;
        this.title = title;
        this.category = category;
        this.awardedOn = awardedOn;
    }
}
