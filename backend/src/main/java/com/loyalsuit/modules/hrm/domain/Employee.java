package com.loyalsuit.modules.hrm.domain;

import com.loyalsuit.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A person who works for the store. Standalone by default — many staff (e.g. a cashier)
 * have no platform login — but can be linked to a user via {@code userId}. The base salary
 * is the monthly figure later HRM slices (leave, payroll) draw on.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee extends TenantScopedEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column
    private String email;

    @Column
    private String phone;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSalary;

    /** A linked login user, if this employee also has an account; null otherwise. */
    @Column(name = "user_id")
    private UUID userId;

    public Employee(UUID tenantId, String fullName, String jobTitle,
                    EmploymentType employmentType, LocalDate hireDate, BigDecimal baseSalary) {
        this.setTenantId(tenantId);
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.employmentType = employmentType;
        this.hireDate = hireDate;
        this.baseSalary = baseSalary;
    }
}
