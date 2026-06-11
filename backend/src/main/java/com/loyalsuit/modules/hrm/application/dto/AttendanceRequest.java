package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Admin-entered (or corrected) attendance for one employee on one day. Check-in / check-out
 * are optional — an admin may mark a day ABSENT with neither, or record only a check-in.
 */
@Data
public class AttendanceRequest {

    @NotNull(message = "An employee is required")
    private UUID employeeId;

    @NotNull(message = "A work date is required")
    private LocalDate workDate;

    @NotNull(message = "A status is required")
    private AttendanceStatus status;

    private Instant checkIn;

    private Instant checkOut;

    @Size(max = 500)
    private String note;
}
