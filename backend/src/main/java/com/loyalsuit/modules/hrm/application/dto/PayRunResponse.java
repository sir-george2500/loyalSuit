package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.PayRun;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PayRunResponse(
        UUID id,
        String label,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        Instant paidAt,
        int payslipCount,
        BigDecimal totalNet,
        Instant createdAt) {

    public static PayRunResponse from(PayRun run) {
        return new PayRunResponse(run.getId(), run.getLabel(), run.getPeriodStart(), run.getPeriodEnd(),
                run.getStatus().name(), run.getPaidAt(), run.getPayslipCount(), run.getTotalNet(), run.getCreatedAt());
    }
}
