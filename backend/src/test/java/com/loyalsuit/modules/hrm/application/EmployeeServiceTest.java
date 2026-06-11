package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.hrm.application.dto.EmployeeRequest;
import com.loyalsuit.modules.hrm.application.dto.EmployeeResponse;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmploymentType;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private HrmAccess hrmAccess;

    @InjectMocks private EmployeeService service;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    private EmployeeRequest request() {
        EmployeeRequest request = new EmployeeRequest();
        request.setFullName("Casey Clerk");
        request.setJobTitle("Cashier");
        request.setEmploymentType(EmploymentType.FULL_TIME);
        request.setHireDate(LocalDate.of(2026, 1, 1));
        request.setBaseSalary(new BigDecimal("2500.00"));
        return request;
    }

    @Test
    void create_addsAnEmployee_defaultingToActive() {
        // Arrange — HRM is available (require is a no-op)
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        EmployeeResponse employee = service.create(tenantId, request());

        // Assert
        assertThat(employee.fullName()).isEqualTo("Casey Clerk");
        assertThat(employee.status()).isEqualTo("ACTIVE");
        assertThat(employee.baseSalary()).isEqualByComparingTo("2500.00");
    }

    @Test
    void create_isBlocked_whenHrmIsNotOnThePlan() {
        // Arrange — the plan gate rejects
        doThrow(new BusinessException("upgrade", HttpStatus.FORBIDDEN)).when(hrmAccess).require(tenantId);

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request()))
                .isInstanceOf(BusinessException.class);
        verify(employeeRepository, never()).save(any());
    }
}
