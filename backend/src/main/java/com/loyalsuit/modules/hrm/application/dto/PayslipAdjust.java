package com.loyalsuit.modules.hrm.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** An owner adjustment to a draft payslip: an extra deduction and an optional note. */
@Data
public class PayslipAdjust {

    @NotNull(message = "An amount is required")
    @DecimalMin(value = "0.00", message = "Deductions can't be negative")
    private BigDecimal otherDeductions;

    @Size(max = 500)
    private String note;
}
