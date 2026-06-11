package com.loyalsuit.modules.hrm.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.hrm.application.dto.EmployeeRequest;
import com.loyalsuit.modules.hrm.application.dto.EmployeeResponse;
import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmployeeStatus;
import com.loyalsuit.modules.hrm.domain.port.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * The employee roster. Owner-managed records of who works for the store. Every operation is
 * gated by {@link HrmAccess} (HRM is a paid plan feature) and tenant-scoped.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final HrmAccess hrmAccess;

    @Transactional
    public EmployeeResponse create(UUID tenantId, EmployeeRequest request) {
        hrmAccess.require(tenantId);
        Employee employee = new Employee(tenantId, request.getFullName().trim(), request.getJobTitle().trim(),
                request.getEmploymentType(), request.getHireDate(), request.getBaseSalary());
        apply(employee, request);
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(UUID id, UUID tenantId, EmployeeRequest request) {
        hrmAccess.require(tenantId);
        Employee employee = load(id, tenantId);
        employee.setFullName(request.getFullName().trim());
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setEmploymentType(request.getEmploymentType());
        employee.setHireDate(request.getHireDate());
        employee.setBaseSalary(request.getBaseSalary());
        apply(employee, request);
        if (request.getStatus() != null) {
            employee.setStatus(request.getStatus());
        }
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public PageResponse<EmployeeResponse> list(UUID tenantId, EmployeeStatus status, Pageable pageable) {
        hrmAccess.require(tenantId);
        var page = status == null
                ? employeeRepository.findByTenantId(tenantId, pageable)
                : employeeRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return new PageResponse<>(page.map(EmployeeResponse::from));
    }

    private static void apply(Employee employee, EmployeeRequest request) {
        employee.setEmail(trimToNull(request.getEmail()));
        employee.setPhone(trimToNull(request.getPhone()));
        employee.setDepartment(trimToNull(request.getDepartment()));
    }

    private Employee load(UUID id, UUID tenantId) {
        return employeeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", id));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
