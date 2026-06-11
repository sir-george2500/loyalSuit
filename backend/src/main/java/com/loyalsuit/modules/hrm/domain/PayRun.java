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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A pay run over a period. Generating it produces one {@link Payslip} per active employee.
 * It moves DRAFT → FINALISED (locked) → PAID; figures on its payslips are snapshotted so a
 * later salary or leave change can't rewrite a finalised run.
 */
@Entity
@Table(name = "pay_runs")
@Getter
@Setter
@NoArgsConstructor
public class PayRun extends TenantScopedEntity {

    @Column(nullable = false)
    private String label;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayRunStatus status = PayRunStatus.DRAFT;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Denormalised run totals, kept current as payslips are generated/adjusted (cheap listing). */
    @Column(name = "payslip_count", nullable = false)
    private int payslipCount;

    @Column(name = "total_net", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    public PayRun(UUID tenantId, String label, LocalDate periodStart, LocalDate periodEnd) {
        this.setTenantId(tenantId);
        this.label = label;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }
}
