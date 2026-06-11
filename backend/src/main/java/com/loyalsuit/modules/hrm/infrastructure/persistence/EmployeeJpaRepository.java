package com.loyalsuit.modules.hrm.infrastructure.persistence;

import com.loyalsuit.modules.hrm.domain.Employee;
import com.loyalsuit.modules.hrm.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EmployeeJpaRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Employee> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Employee> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, EmployeeStatus status, Pageable pageable);
    List<Employee> findByTenantIdAndStatusOrderByFullNameAsc(UUID tenantId, EmployeeStatus status);
}
