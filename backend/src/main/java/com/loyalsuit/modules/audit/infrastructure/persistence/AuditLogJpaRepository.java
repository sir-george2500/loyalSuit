package com.loyalsuit.modules.audit.infrastructure.persistence;

import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface AuditLogJpaRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.tenantId = :tenantId
              AND (:action IS NULL OR a.action = :action)
            ORDER BY a.occurredAt DESC
            """)
    Page<AuditLog> search(@Param("tenantId") UUID tenantId,
                          @Param("action") AuditAction action,
                          Pageable pageable);
}
