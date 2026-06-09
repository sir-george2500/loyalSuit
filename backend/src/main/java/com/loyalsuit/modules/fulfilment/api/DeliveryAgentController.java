package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.fulfilment.application.DeliveryAgentService;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryAgentResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The delivery agent's own view. A courier reads their roster profile here; the admin
 * roster lives under {@code /api/v1/admin/delivery-agents}.
 */
@RestController
@RequestMapping("/api/v1/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery (agent)", description = "A delivery agent's own profile")
public class DeliveryAgentController {

    private final DeliveryAgentService agentService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    @Operation(summary = "My delivery-agent profile")
    public ResponseEntity<ApiResponse<DeliveryAgentResponse>> me(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(agentService.getMine(principal.getUserId())));
    }
}
