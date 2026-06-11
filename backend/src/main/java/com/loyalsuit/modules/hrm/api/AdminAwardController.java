package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.hrm.application.AwardService;
import com.loyalsuit.modules.hrm.application.dto.AwardRequest;
import com.loyalsuit.modules.hrm.application.dto.AwardResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin HRM — employee awards / recognition. Owner-only (SUPER_ADMIN / TENANT_ADMIN); the
 * service also gates by subscription plan, so a BASIC tenant gets a 403 telling them to upgrade.
 */
@RestController
@RequestMapping("/api/v1/admin/awards")
@RequiredArgsConstructor
@Tag(name = "Awards (admin)", description = "HRM employee recognition")
public class AdminAwardController {

    private final AwardService awardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List an employee's awards")
    public ResponseEntity<ApiResponse<List<AwardResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(awardService.list(principal.getTenantId(), employeeId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Record an award for an employee")
    public ResponseEntity<ApiResponse<AwardResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AwardRequest body) {
        AwardResponse award = awardService.create(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(award));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Remove an award")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        awardService.delete(principal.getTenantId(), id);
        return ResponseEntity.ok(ApiResponse.message("Award removed"));
    }
}
