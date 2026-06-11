package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.AwardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** Record an award for an employee. */
@Data
public class AwardRequest {

    @NotNull(message = "An employee is required")
    private UUID employeeId;

    @NotBlank(message = "A title is required")
    @Size(max = 150)
    private String title;

    @NotNull(message = "A category is required")
    private AwardCategory category;

    @NotNull(message = "An award date is required")
    private LocalDate awardedOn;

    @Size(max = 1000)
    private String description;

    @Size(max = 150)
    private String awardedBy;
}
