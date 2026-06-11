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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One employee's attendance for one day. At most one record per employee per work date
 * (enforced by a unique index). Check-in / check-out are stamped on clock actions or entered
 * by an admin; worked hours are derived from them, never stored, so they can't drift.
 */
@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
public class Attendance extends TenantScopedEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in")
    private Instant checkIn;

    @Column(name = "check_out")
    private Instant checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "note")
    private String note;

    public Attendance(UUID tenantId, UUID employeeId, LocalDate workDate, AttendanceStatus status) {
        this.setTenantId(tenantId);
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.status = status;
    }

    /** Worked hours derived from check-in/out (2dp), or zero until both are set. */
    public BigDecimal workedHours() {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long minutes = Duration.between(checkIn, checkOut).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
