package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.hrm.application.dto.PayRunCreate;
import com.loyalsuit.modules.hrm.application.dto.PayRunDetailResponse;
import com.loyalsuit.modules.hrm.application.dto.PayRunResponse;
import com.loyalsuit.modules.hrm.application.dto.PayslipAdjust;
import com.loyalsuit.modules.hrm.application.dto.PayslipResponse;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmploymentType;
import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.PayRun;
import com.loyalsuit.modules.hrm.domain.PayRunStatus;
import com.loyalsuit.modules.hrm.domain.Payslip;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveRequestRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import com.loyalsuit.modules.hrm.domain.port.PayRunRepository;
import com.loyalsuit.modules.hrm.domain.port.PayslipRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private PayRunRepository payRunRepository;
    @Mock private PayslipRepository payslipRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private HrmAccess hrmAccess;

    @InjectMocks private PayrollService service;

    private UUID tenantId;
    private UUID employeeId;
    private UUID unpaidTypeId;
    private final LocalDate start = LocalDate.of(2026, 6, 1);
    private final LocalDate end = LocalDate.of(2026, 6, 30);

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        unpaidTypeId = UUID.randomUUID();
    }

    private PayRunCreate create() {
        PayRunCreate create = new PayRunCreate();
        create.setPeriodStart(start);
        create.setPeriodEnd(end);
        return create;
    }

    private Employee employee(String base) {
        Employee e = new Employee(tenantId, "Casey Clerk", "Cashier",
                EmploymentType.FULL_TIME, LocalDate.of(2026, 1, 1), new BigDecimal(base));
        e.setId(employeeId);
        return e;
    }

    private LeaveType unpaidType() {
        LeaveType t = new LeaveType(tenantId, "Unpaid", 0, false);
        t.setId(unpaidTypeId);
        return t;
    }

    private LeaveRequest approvedUnpaid(LocalDate from, LocalDate to) {
        LeaveRequest r = new LeaveRequest(tenantId, employeeId, unpaidTypeId, from, to, null);
        r.setId(UUID.randomUUID());
        r.setStatus(LeaveStatus.APPROVED);
        return r;
    }

    private PayRun draftRun() {
        PayRun run = new PayRun(tenantId, "June 2026", start, end);
        run.setId(UUID.randomUUID());
        return run;
    }

    @Test
    void generate_pricesUnpaidLeaveIntoEachPayslip() {
        // Arrange — one employee on 3000/month, 3 unpaid-leave days in a 30-day period
        when(payRunRepository.existsForPeriod(tenantId, start, end)).thenReturn(false);
        when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> inv.getArgument(0));
        when(employeeRepository.findActiveByTenantId(tenantId)).thenReturn(List.of(employee("3000.00")));
        when(leaveTypeRepository.findByTenantId(tenantId)).thenReturn(List.of(unpaidType()));
        when(leaveRequestRepository.findApprovedOverlapping(tenantId, start, end))
                .thenReturn(List.of(approvedUnpaid(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 12))));
        when(payslipRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PayRunDetailResponse detail = service.generate(tenantId, create());

        // Assert — daily rate 100, 3 days = 300 deduction, net 2700
        assertThat(detail.payslips()).hasSize(1);
        PayslipResponse slip = detail.payslips().get(0);
        assertThat(slip.unpaidLeaveDays()).isEqualTo(3);
        assertThat(slip.unpaidLeaveDeduction()).isEqualByComparingTo("300.00");
        assertThat(slip.netPay()).isEqualByComparingTo("2700.00");
        assertThat(detail.run().totalNet()).isEqualByComparingTo("2700.00");
        assertThat(detail.run().payslipCount()).isEqualTo(1);
    }

    @Test
    void generate_isRejected_whenARunAlreadyExistsForThePeriod() {
        // Arrange
        when(payRunRepository.existsForPeriod(tenantId, start, end)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.generate(tenantId, create()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(payRunRepository, never()).save(any());
    }

    @Test
    void adjust_recomputesNet_onADraftRun() {
        // Arrange — a 3000 slip with no prior deduction, on a draft run
        PayRun run = draftRun();
        Payslip slip = new Payslip(tenantId, run.getId(), employeeId, "Casey Clerk", new BigDecimal("3000.00"));
        slip.recomputeNet();
        slip.setId(UUID.randomUUID());
        when(payslipRepository.findByIdAndTenantId(slip.getId(), tenantId)).thenReturn(Optional.of(slip));
        when(payRunRepository.findByIdAndTenantId(run.getId(), tenantId)).thenReturn(Optional.of(run));
        when(payslipRepository.findByPayRunId(run.getId())).thenReturn(List.of(slip));
        lenient().when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> inv.getArgument(0));

        PayslipAdjust adjust = new PayslipAdjust();
        adjust.setOtherDeductions(new BigDecimal("200.00"));

        // Act
        PayslipResponse response = service.adjust(tenantId, slip.getId(), adjust);

        // Assert
        assertThat(response.otherDeductions()).isEqualByComparingTo("200.00");
        assertThat(response.netPay()).isEqualByComparingTo("2800.00");
    }

    @Test
    void adjust_isRejected_whenTheRunIsLocked() {
        // Arrange — the run is finalised
        PayRun run = draftRun();
        run.setStatus(PayRunStatus.FINALISED);
        Payslip slip = new Payslip(tenantId, run.getId(), employeeId, "Casey Clerk", new BigDecimal("3000.00"));
        slip.setId(UUID.randomUUID());
        when(payslipRepository.findByIdAndTenantId(slip.getId(), tenantId)).thenReturn(Optional.of(slip));
        when(payRunRepository.findByIdAndTenantId(run.getId(), tenantId)).thenReturn(Optional.of(run));

        PayslipAdjust adjust = new PayslipAdjust();
        adjust.setOtherDeductions(new BigDecimal("200.00"));

        // Act & Assert
        assertThatThrownBy(() -> service.adjust(tenantId, slip.getId(), adjust))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("locked");
        verify(payslipRepository, never()).save(any());
    }

    @Test
    void finalise_locksADraft() {
        PayRun run = draftRun();
        when(payRunRepository.findByIdAndTenantId(run.getId(), tenantId)).thenReturn(Optional.of(run));
        when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> inv.getArgument(0));

        PayRunResponse response = service.finalise(tenantId, run.getId());

        assertThat(response.status()).isEqualTo("FINALISED");
    }

    @Test
    void pay_isRejected_whenTheRunIsNotFinalised() {
        // Arrange — still a draft
        PayRun run = draftRun();
        when(payRunRepository.findByIdAndTenantId(run.getId(), tenantId)).thenReturn(Optional.of(run));

        // Act & Assert
        assertThatThrownBy(() -> service.pay(tenantId, run.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finalised");
        verify(payRunRepository, never()).save(any());
    }

    @Test
    void pay_marksAFinalisedRunPaid() {
        PayRun run = draftRun();
        run.setStatus(PayRunStatus.FINALISED);
        when(payRunRepository.findByIdAndTenantId(run.getId(), tenantId)).thenReturn(Optional.of(run));
        when(payRunRepository.save(any(PayRun.class))).thenAnswer(inv -> inv.getArgument(0));

        PayRunResponse response = service.pay(tenantId, run.getId());

        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    void generate_isBlocked_whenHrmIsNotOnThePlan() {
        doThrow(new BusinessException("upgrade", HttpStatus.FORBIDDEN)).when(hrmAccess).require(tenantId);

        assertThatThrownBy(() -> service.generate(tenantId, create())).isInstanceOf(BusinessException.class);
        verify(payRunRepository, never()).save(any());
    }
}
