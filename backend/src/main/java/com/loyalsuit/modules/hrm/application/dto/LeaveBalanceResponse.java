package com.loyalsuit.modules.hrm.application.dto;

import java.util.UUID;

/** An employee's standing for one leave type in a given year. */
public record LeaveBalanceResponse(
        UUID leaveTypeId,
        String leaveTypeName,
        int year,
        int allowanceDays,
        int takenDays,
        int remainingDays) {
}
