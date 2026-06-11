package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.hrm.application.dto.PayRunCreate;
import com.loyalsuit.modules.hrm.application.dto.PayRunDetailResponse;
import com.loyalsuit.modules.hrm.application.dto.PayRunResponse;
import com.loyalsuit.modules.hrm.application.dto.PayslipAdjust;
import com.loyalsuit.modules.hrm.application.dto.PayslipResponse;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.PayRun;
import com.loyalsuit.modules.hrm.domain.PayRunStatus;
import com.loyalsuit.modules.hrm.domain.Payslip;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveRequestRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import com.loyalsuit.modules.hrm.domain.port.PayRunRepository;
import com.loyalsuit.modules.hrm.domain.port.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payroll. A {@link PayRun} over a period generates one {@link Payslip} per active employee:
 * gross is the monthly base salary, a daily rate prices unpaid-leave days into a deduction, and
 * an owner may add a further adjustment while the run is a draft. Net is base minus both
 * deductions, floored at zero. Finalising locks the run; paying stamps it. Every figure is
 * snapshotted on the slip. Plan-gated via {@link HrmAccess} and tenant-scoped throughout.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollService {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final PayRunRepository payRunRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final HrmAccess hrmAccess;

    @Transactional
    public PayRunDetailResponse generate(UUID tenantId, PayRunCreate create) {
        hrmAccess.require(tenantId);
        LocalDate start = create.getPeriodStart();
        LocalDate end = create.getPeriodEnd();
        if (end.isBefore(start)) {
            throw new BusinessException("The period end can't be before the start", HttpStatus.BAD_REQUEST);
        }
        if (payRunRepository.existsForPeriod(tenantId, start, end)) {
            throw new BusinessException("A pay run already exists for that period", HttpStatus.CONFLICT);
        }

        String label = StringUtils.hasText(create.getLabel()) ? create.getLabel().trim() : start.format(LABEL);
        PayRun run = payRunRepository.save(new PayRun(tenantId, label, start, end));

        long daysInPeriod = ChronoUnit.DAYS.between(start, end) + 1;
        Map<UUID, Integer> unpaidDaysByEmployee = unpaidLeaveDays(tenantId, start, end);

        List<Payslip> slips = employeeRepository.findActiveByTenantId(tenantId).stream()
                .map(e -> slipFor(tenantId, run.getId(), e, daysInPeriod, unpaidDaysByEmployee.getOrDefault(e.getId(), 0)))
                .toList();
        payslipRepository.saveAll(slips);

        run.setPayslipCount(slips.size());
        run.setTotalNet(sumNet(slips));
        payRunRepository.save(run);
        return new PayRunDetailResponse(PayRunResponse.from(run), slips.stream().map(PayslipResponse::from).toList());
    }

    @Transactional
    public PayslipResponse adjust(UUID tenantId, UUID payslipId, PayslipAdjust adjust) {
        hrmAccess.require(tenantId);
        Payslip slip = payslipRepository.findByIdAndTenantId(payslipId, tenantId)
                .orElseThrow(() -> new NotFoundException("Payslip", payslipId));
        PayRun run = loadRun(tenantId, slip.getPayRunId());
        if (run.getStatus() != PayRunStatus.DRAFT) {
            throw new BusinessException("This run is locked and can't be edited", HttpStatus.CONFLICT);
        }
        slip.setOtherDeductions(adjust.getOtherDeductions());
        slip.setNote(StringUtils.hasText(adjust.getNote()) ? adjust.getNote().trim() : null);
        slip.recomputeNet();
        payslipRepository.save(slip);

        run.setTotalNet(sumNet(payslipRepository.findByPayRunId(run.getId())));
        payRunRepository.save(run);
        return PayslipResponse.from(slip);
    }

    @Transactional
    public PayRunResponse finalise(UUID tenantId, UUID runId) {
        hrmAccess.require(tenantId);
        PayRun run = loadRun(tenantId, runId);
        if (run.getStatus() != PayRunStatus.DRAFT) {
            throw new BusinessException("Only a draft run can be finalised", HttpStatus.CONFLICT);
        }
        run.setStatus(PayRunStatus.FINALISED);
        return PayRunResponse.from(payRunRepository.save(run));
    }

    @Transactional
    public PayRunResponse pay(UUID tenantId, UUID runId) {
        hrmAccess.require(tenantId);
        PayRun run = loadRun(tenantId, runId);
        if (run.getStatus() != PayRunStatus.FINALISED) {
            throw new BusinessException("Only a finalised run can be marked paid", HttpStatus.CONFLICT);
        }
        run.setStatus(PayRunStatus.PAID);
        run.setPaidAt(Instant.now());
        return PayRunResponse.from(payRunRepository.save(run));
    }

    public PageResponse<PayRunResponse> list(UUID tenantId, Pageable pageable) {
        hrmAccess.require(tenantId);
        return new PageResponse<>(payRunRepository.findByTenantId(tenantId, pageable).map(PayRunResponse::from));
    }

    public PayRunDetailResponse get(UUID tenantId, UUID runId) {
        hrmAccess.require(tenantId);
        PayRun run = loadRun(tenantId, runId);
        List<PayslipResponse> slips = payslipRepository.findByPayRunId(runId).stream()
                .map(PayslipResponse::from).toList();
        return new PayRunDetailResponse(PayRunResponse.from(run), slips);
    }

    /** Approved unpaid-leave days per employee, clamped to the run's period. */
    private Map<UUID, Integer> unpaidLeaveDays(UUID tenantId, LocalDate start, LocalDate end) {
        Set<UUID> unpaidTypeIds = leaveTypeRepository.findByTenantId(tenantId).stream()
                .filter(t -> !t.isPaid())
                .map(LeaveType::getId)
                .collect(Collectors.toSet());
        if (unpaidTypeIds.isEmpty()) {
            return Map.of();
        }
        return leaveRequestRepository.findApprovedOverlapping(tenantId, start, end).stream()
                .filter(r -> unpaidTypeIds.contains(r.getLeaveTypeId()))
                .collect(Collectors.groupingBy(LeaveRequest::getEmployeeId,
                        Collectors.summingInt(r -> overlapDays(r, start, end))));
    }

    private static int overlapDays(LeaveRequest r, LocalDate start, LocalDate end) {
        LocalDate from = r.getStartDate().isAfter(start) ? r.getStartDate() : start;
        LocalDate to = r.getEndDate().isBefore(end) ? r.getEndDate() : end;
        return to.isBefore(from) ? 0 : (int) (ChronoUnit.DAYS.between(from, to) + 1);
    }

    private Payslip slipFor(UUID tenantId, UUID runId, Employee e, long daysInPeriod, int unpaidDays) {
        Payslip slip = new Payslip(tenantId, runId, e.getId(), e.getFullName(), e.getBaseSalary());
        slip.setUnpaidLeaveDays(unpaidDays);
        if (unpaidDays > 0) {
            BigDecimal dailyRate = e.getBaseSalary().divide(BigDecimal.valueOf(daysInPeriod), 10, RoundingMode.HALF_UP);
            BigDecimal deduction = dailyRate.multiply(BigDecimal.valueOf(unpaidDays)).setScale(2, RoundingMode.HALF_UP);
            slip.setUnpaidLeaveDeduction(deduction.min(e.getBaseSalary()));
        }
        slip.recomputeNet();
        return slip;
    }

    private PayRun loadRun(UUID tenantId, UUID runId) {
        return payRunRepository.findByIdAndTenantId(runId, tenantId)
                .orElseThrow(() -> new NotFoundException("Pay run", runId));
    }

    private static BigDecimal sumNet(List<Payslip> slips) {
        return slips.stream().map(Payslip::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
