package com.loyalsuit.modules.audit.application.dto;

import com.loyalsuit.modules.audit.domain.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String outcome,
        UUID actorId,
        String actorEmail,
        String actorRole,
        String resourceType,
        String resourceId,
        String ipAddress,
        String detail,
        Instant occurredAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAction().name(),
                log.getOutcome().name(),
                log.getActorId(),
                log.getActorEmail(),
                log.getActorRole(),
                log.getResourceType(),
                log.getResourceId(),
                log.getIpAddress(),
                log.getDetail(),
                log.getOccurredAt());
    }
}
