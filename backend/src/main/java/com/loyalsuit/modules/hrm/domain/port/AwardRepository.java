package com.loyalsuit.modules.hrm.domain.port;

import com.loyalsuit.modules.hrm.domain.Award;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AwardRepository {
    Award save(Award award);
    Optional<Award> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Award> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
    void delete(Award award);
}
