package com.loyalsuit.modules.hrm.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A tenant-defined category of leave (e.g. Annual, Sick, Unpaid) with an annual day allowance.
 * The allowance is the per-employee yearly entitlement against which approved leave is drawn.
 */
@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
public class LeaveType extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "annual_allowance_days", nullable = false)
    private int annualAllowanceDays;

    /** Whether leave of this type is paid; informational for now, used by payroll later. */
    @Column(nullable = false)
    private boolean paid = true;

    @Column(nullable = false)
    private boolean active = true;

    public LeaveType(UUID tenantId, String name, int annualAllowanceDays, boolean paid) {
        this.setTenantId(tenantId);
        this.name = name;
        this.annualAllowanceDays = annualAllowanceDays;
        this.paid = paid;
    }
}
