package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.DeliveryService;
import com.loyalsuit.modules.fulfilment.application.dto.AdvanceDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.AssignDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.CompleteDeliveryRequest;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryResponse;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Dispatch desk: assign orders to couriers and move deliveries along their lifecycle.
 * Operational, so it's open to store-side staff (SUPER_ADMIN / TENANT_ADMIN / STAFF) —
 * unlike roster management, which grants roles and is owner-only.
 */
@RestController
@RequestMapping("/api/v1/admin/deliveries")
@RequiredArgsConstructor
@Tag(name = "Deliveries (admin)", description = "Assignment & delivery lifecycle")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "List deliveries (optionally by status)")
    public ResponseEntity<ApiResponse<PageResponse<DeliveryResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) DeliveryStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.list(principal.getTenantId(), status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "A delivery with its status timeline")
    public ResponseEntity<ApiResponse<DeliveryResponse>> get(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(deliveryService.get(id, principal.getTenantId())));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Assign an order's delivery to a courier")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assign(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssignDeliveryRequest body) {
        DeliveryResponse delivery = deliveryService.assign(principal.getTenantId(), principal.getUserId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(delivery));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Advance a delivery to the next status")
    public ResponseEntity<ApiResponse<DeliveryResponse>> advance(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AdvanceDeliveryRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                deliveryService.advance(id, principal.getTenantId(), principal.getUserId(), body)));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Complete a delivery with proof (settles a cash-on-delivery order)")
    public ResponseEntity<ApiResponse<DeliveryResponse>> complete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CompleteDeliveryRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(
                deliveryService.complete(id, principal.getTenantId(), principal.getUserId(), body)));
    }
}
