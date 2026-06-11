package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.hrm.application.dto.LeaveTypeRequest;
import com.loyalsuit.modules.hrm.application.dto.LeaveTypeResponse;
import com.loyalsuit.modules.hrm.domain.LeaveType;
import com.loyalsuit.modules.hrm.domain.port.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Leave types — the tenant-defined categories (Annual, Sick, …) and their annual allowances.
 * Owner-managed and plan-gated via {@link HrmAccess}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final HrmAccess hrmAccess;

    @Transactional
    public LeaveTypeResponse create(UUID tenantId, LeaveTypeRequest request) {
        hrmAccess.require(tenantId);
        LeaveType type = new LeaveType(tenantId, request.getName().trim(),
                request.getAnnualAllowanceDays(), request.getPaid() == null || request.getPaid());
        if (request.getActive() != null) {
            type.setActive(request.getActive());
        }
        return LeaveTypeResponse.from(leaveTypeRepository.save(type));
    }

    @Transactional
    public LeaveTypeResponse update(UUID id, UUID tenantId, LeaveTypeRequest request) {
        hrmAccess.require(tenantId);
        LeaveType type = leaveTypeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Leave type", id));
        type.setName(request.getName().trim());
        type.setAnnualAllowanceDays(request.getAnnualAllowanceDays());
        if (request.getPaid() != null) {
            type.setPaid(request.getPaid());
        }
        if (request.getActive() != null) {
            type.setActive(request.getActive());
        }
        return LeaveTypeResponse.from(leaveTypeRepository.save(type));
    }

    public List<LeaveTypeResponse> list(UUID tenantId) {
        hrmAccess.require(tenantId);
        return leaveTypeRepository.findByTenantId(tenantId).stream().map(LeaveTypeResponse::from).toList();
    }
}
