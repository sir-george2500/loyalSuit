package com.loyalsuit.modules.audit.domain.port;

import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog log);

    /** Most-recent-first audit entries for a tenant, optionally filtered by action. */
    Page<AuditLog> search(UUID tenantId, AuditAction action, Pageable pageable);
}
