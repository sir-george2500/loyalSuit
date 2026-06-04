package com.loyalsuit.modules.audit.application;

import java.util.UUID;

/**
 * Who performed an audited action. Any field may be null for pre-auth events
 * (e.g. a failed login where only the attempted email is known).
 */
public record AuditActor(UUID tenantId, UUID userId, String email, String role) {

    public static AuditActor of(UUID tenantId, UUID userId, String email, String role) {
        return new AuditActor(tenantId, userId, email, role);
    }

    /** An actor known only by the email they typed (e.g. failed/unknown login). */
    public static AuditActor email(UUID tenantId, String email) {
        return new AuditActor(tenantId, null, email, null);
    }
}
