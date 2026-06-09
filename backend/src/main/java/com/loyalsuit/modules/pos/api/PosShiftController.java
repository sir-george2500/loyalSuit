package com.loyalsuit.modules.pos.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.pos.application.PosShiftService;
import com.loyalsuit.modules.pos.application.dto.CloseShiftRequest;
import com.loyalsuit.modules.pos.application.dto.OpenShiftRequest;
import com.loyalsuit.modules.pos.application.dto.PosShiftResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Cash-drawer sessions for the till. A cashier opens their own shift and closes it to
 * reconcile the drawer; listing shows the tenant's shift history. Store roles only;
 * the open shift is scoped to the caller, while close/list act by tenant.
 */
@RestController
@RequestMapping("/api/v1/pos/shifts")
@RequiredArgsConstructor
@Tag(name = "POS shifts", description = "Cash-drawer sessions & reconciliation")
public class PosShiftController {

    private final PosShiftService shiftService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "My currently-open shift, or null if none")
    public ResponseEntity<ApiResponse<PosShiftResponse>> current(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                shiftService.current(principal.getTenantId(), principal.getUserId())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Open a drawer with a starting float")
    public ResponseEntity<ApiResponse<PosShiftResponse>> open(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OpenShiftRequest body) {
        PosShiftResponse shift = shiftService.open(
                principal.getTenantId(), principal.getUserId(), body.getOpeningFloat());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(shift));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Close a drawer and reconcile against the counted cash")
    public ResponseEntity<ApiResponse<PosShiftResponse>> close(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CloseShiftRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                shiftService.close(id, principal.getTenantId(), body.getCountedCash())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "List the tenant's shifts (most recent first)")
    public ResponseEntity<ApiResponse<PageResponse<PosShiftResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.list(principal.getTenantId(), pageable)));
    }
}
