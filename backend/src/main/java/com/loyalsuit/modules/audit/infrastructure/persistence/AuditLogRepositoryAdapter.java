package com.loyalsuit.modules.audit.infrastructure.persistence;

import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.audit.domain.AuditLog;
import com.loyalsuit.modules.audit.domain.port.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    @Override
    public AuditLog save(AuditLog log) {
        return jpa.save(log);
    }

    @Override
    public Page<AuditLog> search(UUID tenantId, AuditAction action, Pageable pageable) {
        return jpa.search(tenantId, action, pageable);
    }
}
