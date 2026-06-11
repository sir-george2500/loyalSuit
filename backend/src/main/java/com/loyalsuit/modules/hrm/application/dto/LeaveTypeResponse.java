package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.LeaveType;

import java.time.Instant;
import java.util.UUID;

public record LeaveTypeResponse(
        UUID id,
        String name,
        int annualAllowanceDays,
        boolean paid,
        boolean active,
        Instant createdAt) {

    public static LeaveTypeResponse from(LeaveType t) {
        return new LeaveTypeResponse(t.getId(), t.getName(), t.getAnnualAllowanceDays(),
                t.isPaid(), t.isActive(), t.getCreatedAt());
    }
}
