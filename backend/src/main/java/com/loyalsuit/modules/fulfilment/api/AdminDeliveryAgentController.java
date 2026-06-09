package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.DeliveryAgentService;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryAgentResponse;
import com.loyalsuit.modules.fulfilment.application.dto.RegisterDeliveryAgentRequest;
import com.loyalsuit.modules.fulfilment.application.dto.UpdateDeliveryAgentRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin management of the delivery-agent roster. Onboarding grants the DELIVERY_AGENT
 * role, so — like vendor approval — it's restricted to tenant owners (SUPER_ADMIN /
 * TENANT_ADMIN), not general staff.
 */
@RestController
@RequestMapping("/api/v1/admin/delivery-agents")
@RequiredArgsConstructor
@Tag(name = "Delivery agents (admin)", description = "Courier roster management")
public class AdminDeliveryAgentController {

    private final DeliveryAgentService agentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List delivery agents (optionally by active state)")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryAgentResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.list(principal.getTenantId(), active, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Onboard a user as a delivery agent (grants the DELIVERY_AGENT role)")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> register(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterDeliveryAgentRequest body) {
        DeliveryAgentResponse agent = agentService.register(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(agent));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update a delivery agent's contact details")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateDeliveryAgentRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.update(id, principal.getTenantId(), body)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Reactivate a delivery agent")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> activate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.setActive(id, principal.getTenantId(), true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Deactivate a delivery agent (keeps history; not assignable)")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> deactivate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.setActive(id, principal.getTenantId(), false)));
    }
}
