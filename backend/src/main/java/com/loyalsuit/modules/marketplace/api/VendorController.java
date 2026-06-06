package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.marketplace.application.VendorService;
import com.loyalsuit.modules.marketplace.application.dto.ApplyVendorRequest;
import com.loyalsuit.modules.marketplace.application.dto.VendorResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vendor self-service: any signed-in user can apply to sell, and a vendor can read
 * their own profile/status. Approval is an admin action (see AdminVendorController).
 */
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
@Tag(name = "Vendor", description = "Vendor self-service")
public class VendorController {

    private final VendorService vendorService;

    @PostMapping("/apply")
    @Operation(summary = "Apply to become a vendor")
    public ResponseEntity<ApiResponse<VendorResponse>> apply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ApplyVendorRequest request) {
        VendorResponse response = vendorService.apply(principal.getTenantId(), principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my vendor profile and status")
    public ResponseEntity<ApiResponse<VendorResponse>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.getMine(principal.getUserId())));
    }
}
