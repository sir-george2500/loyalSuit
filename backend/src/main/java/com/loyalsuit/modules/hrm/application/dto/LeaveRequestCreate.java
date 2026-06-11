package com.loyalsuit.modules.hrm.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** Raise a leave request for an employee. */
@Data
public class LeaveRequestCreate {

    @NotNull(message = "An employee is required")
    private UUID employeeId;

    @NotNull(message = "A leave type is required")
    private UUID leaveTypeId;

    @NotNull(message = "A start date is required")
    private LocalDate startDate;

    @NotNull(message = "An end date is required")
    private LocalDate endDate;

    @Size(max = 500)
    private String reason;
}
