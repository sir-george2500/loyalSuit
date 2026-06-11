package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.LeaveRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestResponse(
        UUID id,
        UUID employeeId,
        UUID leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        int days,
        String reason,
        String status,
        String decisionNote,
        Instant createdAt) {

    public static LeaveRequestResponse from(LeaveRequest r, String leaveTypeName) {
        return new LeaveRequestResponse(r.getId(), r.getEmployeeId(), r.getLeaveTypeId(), leaveTypeName,
                r.getStartDate(), r.getEndDate(), r.getDays(), r.getReason(),
                r.getStatus().name(), r.getDecisionNote(), r.getCreatedAt());
    }
}
