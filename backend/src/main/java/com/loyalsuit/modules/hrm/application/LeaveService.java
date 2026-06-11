package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.hrm.application.dto.LeaveBalanceResponse;
import com.loyalsuit.modules.hrm.application.dto.LeaveDecision;
import com.loyalsuit.modules.hrm.application.dto.LeaveRequestCreate;
import com.loyalsuit.modules.hrm.application.dto.LeaveRequestResponse;
import com.loyalsuit.modules.hrm.domain.LeaveRequest;
import com.loyalsuit.modules.hrm.domain.LeaveStatus;
import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveRequestRepository;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Leave requests and balances. An owner raises a request for an employee (PENDING), then
 * approves or rejects it. Approval is balance-checked: a type's annual allowance, less the
 * approved days already taken that year, must cover the request. Every op is plan-gated and
 * tenant-scoped, and confirms the employee and leave type belong to the tenant.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final HrmAccess hrmAccess;

    @Transactional
    public LeaveRequestResponse create(UUID tenantId, LeaveRequestCreate create) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, create.getEmployeeId());
        LeaveType type = requireActiveType(tenantId, create.getLeaveTypeId());
        if (create.getEndDate().isBefore(create.getStartDate())) {
            throw new BusinessException("The end date can't be before the start date", HttpStatus.BAD_REQUEST);
        }
        LeaveRequest request = new LeaveRequest(tenantId, create.getEmployeeId(), create.getLeaveTypeId(),
                create.getStartDate(), create.getEndDate(),
                StringUtils.hasText(create.getReason()) ? create.getReason().trim() : null);
        return LeaveRequestResponse.from(leaveRequestRepository.save(request), type.getName());
    }

    @Transactional
    public LeaveRequestResponse approve(UUID tenantId, UUID id, LeaveDecision decision) {
        hrmAccess.require(tenantId);
        LeaveRequest request = requirePending(tenantId, id);
        LeaveType type = leaveTypeRepository.findByIdAndTenantId(request.getLeaveTypeId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Leave type", request.getLeaveTypeId()));

        int year = request.getStartDate().getYear();
        int taken = takenDays(tenantId, request.getEmployeeId(), request.getLeaveTypeId(), year);
        int remaining = type.getAnnualAllowanceDays() - taken;
        if (request.getDays() > remaining) {
            throw new BusinessException(
                    "Insufficient '%s' balance — %d day(s) remaining, %d requested"
                            .formatted(type.getName(), Math.max(remaining, 0), request.getDays()),
                    HttpStatus.CONFLICT);
        }
        request.setStatus(LeaveStatus.APPROVED);
        request.setDecisionNote(noteOf(decision));
        return LeaveRequestResponse.from(leaveRequestRepository.save(request), type.getName());
    }

    @Transactional
    public LeaveRequestResponse reject(UUID tenantId, UUID id, LeaveDecision decision) {
        hrmAccess.require(tenantId);
        LeaveRequest request = requirePending(tenantId, id);
        request.setStatus(LeaveStatus.REJECTED);
        request.setDecisionNote(noteOf(decision));
        return LeaveRequestResponse.from(leaveRequestRepository.save(request), typeName(tenantId, request.getLeaveTypeId()));
    }

    public PageResponse<LeaveRequestResponse> list(UUID tenantId, LeaveStatus status, Pageable pageable) {
        hrmAccess.require(tenantId);
        var page = status == null
                ? leaveRequestRepository.findByTenantId(tenantId, pageable)
                : leaveRequestRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        Map<UUID, String> names = typeNames(tenantId);
        return new PageResponse<>(page.map(r -> LeaveRequestResponse.from(r, names.getOrDefault(r.getLeaveTypeId(), "—"))));
    }

    public List<LeaveRequestResponse> history(UUID tenantId, UUID employeeId) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, employeeId);
        Map<UUID, String> names = typeNames(tenantId);
        return leaveRequestRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .map(r -> LeaveRequestResponse.from(r, names.getOrDefault(r.getLeaveTypeId(), "—")))
                .toList();
    }

    /** An employee's balance for every active leave type, for the current year. */
    public List<LeaveBalanceResponse> balances(UUID tenantId, UUID employeeId) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, employeeId);
        int year = LocalDate.now().getYear();
        Map<UUID, Integer> takenByType = leaveRequestRepository
                .findApproved(tenantId, employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .collect(Collectors.groupingBy(LeaveRequest::getLeaveTypeId, Collectors.summingInt(LeaveRequest::getDays)));
        return leaveTypeRepository.findActiveByTenantId(tenantId).stream()
                .map(t -> {
                    int taken = takenByType.getOrDefault(t.getId(), 0);
                    return new LeaveBalanceResponse(t.getId(), t.getName(), year,
                            t.getAnnualAllowanceDays(), taken, t.getAnnualAllowanceDays() - taken);
                })
                .toList();
    }

    private int takenDays(UUID tenantId, UUID employeeId, UUID leaveTypeId, int year) {
        return leaveRequestRepository
                .findApproved(tenantId, employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .filter(r -> r.getLeaveTypeId().equals(leaveTypeId))
                .mapToInt(LeaveRequest::getDays)
                .sum();
    }

    private LeaveRequest requirePending(UUID tenantId, UUID id) {
        LeaveRequest request = leaveRequestRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Leave request", id));
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("This request has already been decided", HttpStatus.CONFLICT);
        }
        return request;
    }

    private void requireEmployee(UUID tenantId, UUID employeeId) {
        employeeRepository.findByIdAndTenantId(employeeId, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", employeeId));
    }

    private LeaveType requireActiveType(UUID tenantId, UUID leaveTypeId) {
        LeaveType type = leaveTypeRepository.findByIdAndTenantId(leaveTypeId, tenantId)
                .orElseThrow(() -> new NotFoundException("Leave type", leaveTypeId));
        if (!type.isActive()) {
            throw new BusinessException("That leave type is no longer active", HttpStatus.BAD_REQUEST);
        }
        return type;
    }

    private Map<UUID, String> typeNames(UUID tenantId) {
        return leaveTypeRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(LeaveType::getId, LeaveType::getName));
    }

    private String typeName(UUID tenantId, UUID leaveTypeId) {
        return leaveTypeRepository.findByIdAndTenantId(leaveTypeId, tenantId)
                .map(LeaveType::getName).orElse("—");
    }

    private static String noteOf(LeaveDecision decision) {
        return decision != null && StringUtils.hasText(decision.getNote()) ? decision.getNote().trim() : null;
    }
}
