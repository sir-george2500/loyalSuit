package com.loyalsuit.modules.audit.application;

import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.audit.domain.AuditLog;
import com.loyalsuit.modules.audit.domain.AuditOutcome;
import com.loyalsuit.modules.audit.domain.port.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Writes and reads the audit trail.
 *
 * <p>Each write runs in its <b>own</b> transaction ({@code REQUIRES_NEW}) and is
 * best-effort: a failure to audit is logged but never propagates. This means the
 * audit of an <em>attempt</em> survives even when the business transaction it
 * describes rolls back — so failed logins and rejected operations are still
 * recorded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, AuditOutcome outcome, AuditActor actor,
                       String resourceType, String resourceId, String detail) {
        try {
            AuditLog entry = AuditLog.builder()
                    .tenantId(actor != null ? actor.tenantId() : null)
                    .actorId(actor != null ? actor.userId() : null)
                    .actorEmail(actor != null ? actor.email() : null)
                    .actorRole(actor != null ? actor.role() : null)
                    .action(action)
                    .outcome(outcome)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ipAddress(clientIp())
                    .userAgent(userAgent())
                    .detail(detail)
                    .occurredAt(Instant.now())
                    .build();
            repository.save(entry);
        } catch (RuntimeException e) {
            // Auditing must never break or roll back the operation it describes.
            log.error("Failed to write audit entry action={} outcome={}: {}", action, outcome, e.getMessage());
        }
    }

    public void recordSuccess(AuditAction action, AuditActor actor, String resourceType,
                              String resourceId, String detail) {
        record(action, AuditOutcome.SUCCESS, actor, resourceType, resourceId, detail);
    }

    public void recordFailure(AuditAction action, AuditActor actor, String resourceType,
                              String resourceId, String detail) {
        record(action, AuditOutcome.FAILURE, actor, resourceType, resourceId, detail);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> query(UUID tenantId, AuditAction action, Pageable pageable) {
        return repository.search(tenantId, action, pageable);
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static String userAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String ua = req.getHeader("User-Agent");
        if (ua != null && ua.length() > 512) {
            return ua.substring(0, 512);
        }
        return ua;
    }
}
