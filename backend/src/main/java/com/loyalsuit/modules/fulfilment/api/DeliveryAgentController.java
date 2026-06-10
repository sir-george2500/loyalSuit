package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.DeliveryAgentService;
import com.loyalsuit.modules.fulfilment.application.DeliveryService;
import com.loyalsuit.modules.fulfilment.application.dto.AdvanceDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryAgentResponse;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * The delivery agent's own view: their roster profile, their active work queue, and
 * advancing the deliveries assigned to them. The admin roster and dispatch desk live
 * under {@code /api/v1/admin/...}. Every action is scoped to the calling courier.
 */
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery (agent)", description = "A delivery agent's profile, queue & updates")
public class DeliveryAgentController {

    private final DeliveryAgentService agentService;
    private final DeliveryService deliveryService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "My delivery-agent profile")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.getMine(principal.getUserId())));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "My active deliveries (open work queue)")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryResponse>>> assignments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                deliveryService.myAssignments(principal.getTenantId(), principal.getUserId(), pageable)));
    }

    @PatchMapping("/assignments/{id}/status")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "Advance one of my deliveries to the next status")
    public ResponseEntity<ApiResponse<DeliveryResponse>> advance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdvanceDeliveryRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                deliveryService.advanceAsAgent(id, principal.getTenantId(), principal.getUserId(), body)));
    }
}
