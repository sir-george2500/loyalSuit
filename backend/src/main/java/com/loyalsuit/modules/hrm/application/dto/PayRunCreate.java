package com.loyalsuit.modules.hrm.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** Open a draft pay run for a period. A blank label defaults to the period dates. */
@Data
public class PayRunCreate {

    @NotNull(message = "A period start is required")
    private LocalDate periodStart;

    @NotNull(message = "A period end is required")
    private LocalDate periodEnd;

    @Size(max = 100)
    private String label;
}
