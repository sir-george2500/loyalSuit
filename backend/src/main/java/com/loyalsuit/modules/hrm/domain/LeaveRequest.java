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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * An employee's request for leave of a given type over an inclusive date range. Starts PENDING;
 * an owner approves or rejects it. The day count is snapshotted at creation from the range so a
 * later type/allowance change can't silently rewrite history.
 */
@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest extends TenantScopedEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int days;

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "decision_note")
    private String decisionNote;

    public LeaveRequest(UUID tenantId, UUID employeeId, UUID leaveTypeId,
                        LocalDate startDate, LocalDate endDate, String reason) {
        this.setTenantId(tenantId);
        this.employeeId = employeeId;
        this.leaveTypeId = leaveTypeId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = countDays(startDate, endDate);
        this.reason = reason;
    }

    /** Inclusive day count across the range (a single-day request is one day). */
    public static int countDays(LocalDate start, LocalDate end) {
        return (int) (ChronoUnit.DAYS.between(start, end) + 1);
    }
}
