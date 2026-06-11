package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.hrm.application.dto.LeaveBalanceResponse;
import com.loyalsuit.modules.hrm.application.dto.LeaveRequestCreate;
import com.loyalsuit.modules.hrm.application.dto.LeaveRequestResponse;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmploymentType;
import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveRequestRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private HrmAccess hrmAccess;

    @InjectMocks private LeaveService service;

    private UUID tenantId;
    private UUID employeeId;
    private UUID typeId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        typeId = UUID.randomUUID();
    }

    private void employeeExists() {
        when(employeeRepository.findByIdAndTenantId(employeeId, tenantId))
                .thenReturn(Optional.of(new Employee(tenantId, "Casey Clerk", "Cashier",
                        EmploymentType.FULL_TIME, LocalDate.of(2026, 1, 1), new BigDecimal("2500.00"))));
    }

    private LeaveType leaveType(int allowanceDays, boolean active) {
        LeaveType type = new LeaveType(tenantId, "Annual", allowanceDays, true);
        type.setId(typeId);
        type.setActive(active);
        return type;
    }

    private LeaveRequestCreate createRequest(LocalDate start, LocalDate end) {
        LeaveRequestCreate create = new LeaveRequestCreate();
        create.setEmployeeId(employeeId);
        create.setLeaveTypeId(typeId);
        create.setStartDate(start);
        create.setEndDate(end);
        return create;
    }

    private LeaveRequest pendingRequest(int allowanceUseDays) {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LeaveRequest request = new LeaveRequest(tenantId, employeeId, typeId, start, start.plusDays(allowanceUseDays - 1), null);
        request.setId(UUID.randomUUID());
        return request;
    }

    @Test
    void create_raisesAPendingRequest_withTheDayCount() {
        // Arrange — a 3-day request against an active type
        employeeExists();
        when(leaveTypeRepository.findByIdAndTenantId(typeId, tenantId)).thenReturn(Optional.of(leaveType(20, true)));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LeaveRequestResponse response = service.create(tenantId,
                createRequest(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3)));

        // Assert
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.days()).isEqualTo(3);
        assertThat(response.leaveTypeName()).isEqualTo("Annual");
    }

    @Test
    void create_rejectsEndBeforeStart() {
        // Arrange
        employeeExists();
        when(leaveTypeRepository.findByIdAndTenantId(typeId, tenantId)).thenReturn(Optional.of(leaveType(20, true)));

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId,
                createRequest(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 1))))
                .isInstanceOf(BusinessException.class);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void create_rejectsAnInactiveType() {
        // Arrange
        employeeExists();
        when(leaveTypeRepository.findByIdAndTenantId(typeId, tenantId)).thenReturn(Optional.of(leaveType(20, false)));

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId,
                createRequest(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer active");
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void approve_setsApproved_whenWithinBalance() {
        // Arrange — a 3-day request, 20-day allowance, none taken
        LeaveRequest request = pendingRequest(3);
        when(leaveRequestRepository.findByIdAndTenantId(request.getId(), tenantId)).thenReturn(Optional.of(request));
        when(leaveTypeRepository.findByIdAndTenantId(typeId, tenantId)).thenReturn(Optional.of(leaveType(20, true)));
        when(leaveRequestRepository.findApproved(any(), any(), any(), any())).thenReturn(List.of());
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        LeaveRequestResponse response = service.approve(tenantId, request.getId(), null);

        // Assert
        assertThat(response.status()).isEqualTo("APPROVED");
    }

    @Test
    void approve_isRejected_whenOverBalance() {
        // Arrange — a 10-day request but only 5 days allowance, none taken
        LeaveRequest request = pendingRequest(10);
        when(leaveRequestRepository.findByIdAndTenantId(request.getId(), tenantId)).thenReturn(Optional.of(request));
        when(leaveTypeRepository.findByIdAndTenantId(typeId, tenantId)).thenReturn(Optional.of(leaveType(5, true)));
        when(leaveRequestRepository.findApproved(any(), any(), any(), any())).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> service.approve(tenantId, request.getId(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void approve_isRejected_whenAlreadyDecided() {
        // Arrange — the request was already approved
        LeaveRequest request = pendingRequest(3);
        request.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findByIdAndTenantId(request.getId(), tenantId)).thenReturn(Optional.of(request));

        // Act & Assert
        assertThatThrownBy(() -> service.approve(tenantId, request.getId(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been decided");
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void balances_subtractApprovedDaysFromTheAllowance() {
        // Arrange — 20-day allowance, 5 approved days taken this year
        employeeExists();
        LeaveRequest taken = pendingRequest(5);
        taken.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findApproved(any(), any(), any(), any())).thenReturn(List.of(taken));
        when(leaveTypeRepository.findActiveByTenantId(tenantId)).thenReturn(List.of(leaveType(20, true)));

        // Act
        List<LeaveBalanceResponse> balances = service.balances(tenantId, employeeId);

        // Assert
        assertThat(balances).hasSize(1);
        assertThat(balances.get(0).takenDays()).isEqualTo(5);
        assertThat(balances.get(0).remainingDays()).isEqualTo(15);
    }

    @Test
    void create_isBlocked_whenHrmIsNotOnThePlan() {
        // Arrange
        doThrow(new BusinessException("upgrade", HttpStatus.FORBIDDEN)).when(hrmAccess).require(tenantId);

        // Act & Assert
        assertThatThrownBy(() -> service.create(tenantId,
                createRequest(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3))))
                .isInstanceOf(BusinessException.class);
        verify(leaveRequestRepository, never()).save(any());
    }
}
