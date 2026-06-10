package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.fulfilment.application.PickupPointService;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryZoneRequest;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryZoneResponse;
import com.loyalsuit.modules.fulfilment.application.dto.PickupPointRequest;
import com.loyalsuit.modules.fulfilment.application.dto.PickupPointResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin configuration of the pickup-and-zones network. Shipping fees flow from these
 * zones, so it's owner-only (SUPER_ADMIN / TENANT_ADMIN). Each point owns its own zones.
 */
@RestController
@RequestMapping("/api/v1/admin/pickup-points")
@RequiredArgsConstructor
@Tag(name = "Pickup points (admin)", description = "Pickup points & their delivery zones")
public class AdminPickupPointController {

    private final PickupPointService pickupPointService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List pickup points with their zones")
    public ResponseEntity<ApiResponse<PageResponse<PickupPointResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.listPoints(principal.getTenantId(), pageable)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Create a pickup point")
    public ResponseEntity<ApiResponse<PickupPointResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PickupPointRequest body) {
        PickupPointResponse point = pickupPointService.createPoint(principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(point));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update a pickup point")
    public ResponseEntity<ApiResponse<PickupPointResponse>> update(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PickupPointRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.updatePoint(id, principal.getTenantId(), body)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Activate a pickup point")
    public ResponseEntity<ApiResponse<PickupPointResponse>> activate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.setPointActive(id, principal.getTenantId(), true)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Deactivate a pickup point (hidden at checkout)")
    public ResponseEntity<ApiResponse<PickupPointResponse>> deactivate(
            @PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.setPointActive(id, principal.getTenantId(), false)));
    }

    // ---- zones --------------------------------------------------------------

    @PostMapping("/{pointId}/zones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Add a delivery zone to a pickup point")
    public ResponseEntity<ApiResponse<DeliveryZoneResponse>> addZone(
            @PathVariable UUID pointId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryZoneRequest body) {
        DeliveryZoneResponse zone = pickupPointService.addZone(pointId, principal.getTenantId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(zone));
    }

    @PutMapping("/zones/{zoneId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update a delivery zone")
    public ResponseEntity<ApiResponse<DeliveryZoneResponse>> updateZone(
            @PathVariable UUID zoneId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DeliveryZoneRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.updateZone(zoneId, principal.getTenantId(), body)));
    }

    @PatchMapping("/zones/{zoneId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Activate a delivery zone")
    public ResponseEntity<ApiResponse<DeliveryZoneResponse>> activateZone(
            @PathVariable UUID zoneId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.setZoneActive(zoneId, principal.getTenantId(), true)));
    }

    @PatchMapping("/zones/{zoneId}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Deactivate a delivery zone")
    public ResponseEntity<ApiResponse<DeliveryZoneResponse>> deactivateZone(
            @PathVariable UUID zoneId, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.setZoneActive(zoneId, principal.getTenantId(), false)));
    }

    @DeleteMapping("/zones/{zoneId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete a delivery zone")
    public ResponseEntity<ApiResponse<Void>> deleteZone(
            @PathVariable UUID zoneId, @AuthenticationPrincipal UserPrincipal principal) {
        pickupPointService.deleteZone(zoneId, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
