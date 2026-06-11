package com.loyalsuit.modules.hrm.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or edit a leave type. */
@Data
public class LeaveTypeRequest {

    @NotBlank(message = "A name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "An annual allowance is required")
    @Min(value = 0, message = "Allowance can't be negative")
    @Max(value = 366, message = "Allowance can't exceed a year")
    private Integer annualAllowanceDays;

    private Boolean paid;

    private Boolean active;
}
