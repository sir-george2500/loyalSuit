package com.loyalsuit.modules.hrm.application.dto;

import com.loyalsuit.modules.hrm.domain.Employee;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String jobTitle,
        String department,
        String employmentType,
        String status,
        LocalDate hireDate,
        BigDecimal baseSalary,
        UUID userId,
        Instant createdAt) {

    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getFullName(),
                e.getEmail(),
                e.getPhone(),
                e.getJobTitle(),
                e.getDepartment(),
                e.getEmploymentType().name(),
                e.getStatus().name(),
                e.getHireDate(),
                e.getBaseSalary(),
                e.getUserId(),
                e.getCreatedAt());
    }
}
