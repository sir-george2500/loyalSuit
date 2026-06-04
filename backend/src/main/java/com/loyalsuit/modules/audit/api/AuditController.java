package com.loyalsuit.modules.audit.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.application.dto.AuditLogResponse;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Tenant-scoped audit trail")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List the current tenant's audit events (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 25) Pageable pageable) {
        PageResponse<AuditLogResponse> page = new PageResponse<>(
                auditService.query(principal.getTenantId(), action, pageable)
                        .map(AuditLogResponse::from));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }
}
