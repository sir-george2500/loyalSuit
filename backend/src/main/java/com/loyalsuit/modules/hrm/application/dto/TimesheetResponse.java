package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.Attendance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** An employee's attendance over a date range, with the days present and total hours worked. */
public record TimesheetResponse(
        UUID employeeId,
        LocalDate from,
        LocalDate to,
        long daysPresent,
        BigDecimal totalHours,
        List<AttendanceResponse> entries) {

    public static TimesheetResponse of(UUID employeeId, LocalDate from, LocalDate to, List<Attendance> records) {
        BigDecimal total = records.stream()
                .map(Attendance::workedHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long present = records.stream().filter(a -> a.getCheckIn() != null).count();
        List<AttendanceResponse> entries = records.stream().map(AttendanceResponse::from).toList();
        return new TimesheetResponse(employeeId, from, to, present, total, entries);
    }
}
