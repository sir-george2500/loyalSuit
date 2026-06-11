package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.hrm.application.dto.AwardRequest;
import com.loyalsuit.modules.hrm.application.dto.AwardResponse;
import com.loyalsuit.modules.hrm.domain.Award;
import com.loyalsuit.modules.hrm.domain.port.AwardRepository;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Employee awards / recognition. Simple standalone records shown on an employee's profile.
 * Owner-managed and plan-gated via {@link HrmAccess}; confirms the employee belongs to the
 * tenant before recording or listing their awards.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AwardService {

    private final AwardRepository awardRepository;
    private final EmployeeRepository employeeRepository;
    private final HrmAccess hrmAccess;

    @Transactional
    public AwardResponse create(UUID tenantId, AwardRequest request) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, request.getEmployeeId());
        Award award = new Award(tenantId, request.getEmployeeId(), request.getTitle().trim(),
                request.getCategory(), request.getAwardedOn());
        award.setDescription(trimToNull(request.getDescription()));
        award.setAwardedBy(trimToNull(request.getAwardedBy()));
        return AwardResponse.from(awardRepository.save(award));
    }

    public List<AwardResponse> list(UUID tenantId, UUID employeeId) {
        hrmAccess.require(tenantId);
        requireEmployee(tenantId, employeeId);
        return awardRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .map(AwardResponse::from).toList();
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        hrmAccess.require(tenantId);
        Award award = awardRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Award", id));
        awardRepository.delete(award);
    }

    private void requireEmployee(UUID tenantId, UUID employeeId) {
        employeeRepository.findByIdAndTenantId(employeeId, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", employeeId));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
