package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.Payslip;

import java.math.BigDecimal;
import java.util.UUID;

public record PayslipResponse(
        UUID id,
        UUID payRunId,
        UUID employeeId,
        String employeeName,
        BigDecimal baseSalary,
        int unpaidLeaveDays,
        BigDecimal unpaidLeaveDeduction,
        BigDecimal otherDeductions,
        BigDecimal netPay,
        String note) {

    public static PayslipResponse from(Payslip p) {
        return new PayslipResponse(p.getId(), p.getPayRunId(), p.getEmployeeId(), p.getEmployeeName(),
                p.getBaseSalary(), p.getUnpaidLeaveDays(), p.getUnpaidLeaveDeduction(),
                p.getOtherDeductions(), p.getNetPay(), p.getNote());
    }
}
