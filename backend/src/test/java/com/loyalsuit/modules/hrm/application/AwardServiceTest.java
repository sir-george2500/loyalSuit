package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.hrm.application.dto.AwardRequest;
import com.loyalsuit.modules.hrm.application.dto.AwardResponse;
import com.loyalsuit.modules.hrm.domain.Award;
import com.loyalsuit.modules.hrm.domain.AwardCategory;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmploymentType;
import com.loyalsuit.modules.hrm.domain.port.AwardRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwardServiceTest {

    @Mock private AwardRepository awardRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private HrmAccess hrmAccess;

    @InjectMocks private AwardService service;

    private UUID tenantId;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
    }

    private void employeeExists() {
        when(employeeRepository.findByIdAndTenantId(employeeId, tenantId))
                .thenReturn(Optional.of(new Employee(tenantId, "Casey Clerk", "Cashier",
                        EmploymentType.FULL_TIME, LocalDate.of(2026, 1, 1), new BigDecimal("2500.00"))));
    }

    private AwardRequest request() {
        AwardRequest request = new AwardRequest();
        request.setEmployeeId(employeeId);
        request.setTitle("Employee of the Month");
        request.setCategory(AwardCategory.PERFORMANCE);
        request.setAwardedOn(LocalDate.of(2026, 6, 1));
        return request;
    }

    @Test
    void create_recordsAnAward() {
        // Arrange
        employeeExists();
        when(awardRepository.save(any(Award.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        AwardResponse award = service.create(tenantId, request());

        // Assert
        assertThat(award.title()).isEqualTo("Employee of the Month");
        assertThat(award.category()).isEqualTo("PERFORMANCE");
        assertThat(award.employeeId()).isEqualTo(employeeId);
    }

    @Test
    void create_anUnknownEmployee_is404() {
        // Arrange — plan is fine, but the employee isn't in this tenant
        when(employeeRepository.findByIdAndTenantId(employeeId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request())).isInstanceOf(NotFoundException.class);
        verify(awardRepository, never()).save(any());
    }

    @Test
    void delete_removesAnExistingAward() {
        // Arrange
        Award award = new Award(tenantId, employeeId, "Award", AwardCategory.MILESTONE, LocalDate.of(2026, 6, 1));
        UUID awardId = UUID.randomUUID();
        award.setId(awardId);
        when(awardRepository.findByIdAndTenantId(awardId, tenantId)).thenReturn(Optional.of(award));

        // Act
        service.delete(tenantId, awardId);

        // Assert
        verify(awardRepository).delete(award);
    }

    @Test
    void create_isBlocked_whenHrmIsNotOnThePlan() {
        // Arrange
        doThrow(new BusinessException("upgrade", HttpStatus.FORBIDDEN)).when(hrmAccess).require(tenantId);

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId, request())).isInstanceOf(BusinessException.class);
        verify(awardRepository, never()).save(any());
    }
}
