package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.marketplace.application.VendorService;
import com.loyalsuit.modules.marketplace.application.dto.ApplyVendorRequest;
import com.loyalsuit.modules.marketplace.application.dto.UpdateStorefrontRequest;
import com.loyalsuit.modules.marketplace.application.dto.VendorResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @PatchMapping("/me")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Update my storefront profile (name, description)")
    public ResponseEntity<ApiResponse<VendorResponse>> updateMine(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateStorefrontRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(vendorService.updateMine(principal.getUserId(), request)));
    }

    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Upload or replace my storefront logo")
    public ResponseEntity<ApiResponse<VendorResponse>> uploadLogo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No file was uploaded");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Could not read the uploaded file");
        }
        return ResponseEntity.ok(ApiResponse.ok(vendorService.updateLogo(principal.getUserId(), data)));
    }
}
