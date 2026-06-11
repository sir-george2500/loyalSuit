package com.loyalsuit.modules.hrm.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * One employee's pay for one run. Everything is snapshotted at generation: the base salary,
 * the unpaid-leave days and their costing, and the employee's name. Net is base minus the
 * unpaid-leave deduction minus any owner adjustment, floored at zero.
 */
@Entity
@Table(name = "payslips")
@Getter
@Setter
@NoArgsConstructor
public class Payslip extends TenantScopedEntity {

    @Column(name = "pay_run_id", nullable = false)
    private UUID payRunId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "unpaid_leave_days", nullable = false)
    private int unpaidLeaveDays;

    @Column(name = "unpaid_leave_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal unpaidLeaveDeduction = BigDecimal.ZERO;

    @Column(name = "other_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Column(name = "note")
    private String note;

    public Payslip(UUID tenantId, UUID payRunId, UUID employeeId, String employeeName, BigDecimal baseSalary) {
        this.setTenantId(tenantId);
        this.payRunId = payRunId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.baseSalary = baseSalary;
    }

    /** Recompute net from the snapshotted gross less both deductions, never below zero. */
    public void recomputeNet() {
        BigDecimal net = baseSalary.subtract(unpaidLeaveDeduction).subtract(otherDeductions);
        this.netPay = net.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
