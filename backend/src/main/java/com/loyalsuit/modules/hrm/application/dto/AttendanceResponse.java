package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.Attendance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID employeeId,
        LocalDate workDate,
        Instant checkIn,
        Instant checkOut,
        BigDecimal hoursWorked,
        String status,
        String note,
        Instant createdAt) {

    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getId(),
                a.getEmployeeId(),
                a.getWorkDate(),
                a.getCheckIn(),
                a.getCheckOut(),
                a.workedHours(),
                a.getStatus().name(),
                a.getNote(),
                a.getCreatedAt());
    }
}
