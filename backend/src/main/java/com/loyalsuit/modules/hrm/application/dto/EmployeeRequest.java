package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.EmployeeStatus;
import com.loyalsuit.modules.hrm.domain.EmploymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Create or edit an employee. Status is optional (defaults to ACTIVE on create). */
@Data
public class EmployeeRequest {

    @NotBlank(message = "A name is required")
    @Size(max = 255)
    private String fullName;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 40)
    private String phone;

    @NotBlank(message = "A job title is required")
    @Size(max = 255)
    private String jobTitle;

    @Size(max = 255)
    private String department;

    @NotNull(message = "An employment type is required")
    private EmploymentType employmentType;

    @NotNull(message = "A hire date is required")
    private LocalDate hireDate;

    @NotNull(message = "A base salary is required")
    @DecimalMin(value = "0.00", message = "Salary can't be negative")
    private BigDecimal baseSalary;

    private EmployeeStatus status;
}
