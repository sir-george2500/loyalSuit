package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Employee> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Employee> findByTenantIdAndStatus(UUID tenantId, EmployeeStatus status, Pageable pageable);
}
