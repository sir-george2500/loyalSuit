package com.loyalsuit.modules.catalog.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.catalog.application.ProductVariantService;
import com.loyalsuit.modules.catalog.application.dto.VariantRequest;
import com.loyalsuit.modules.catalog.application.dto.VariantResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog/products/{productId}/variants")
@RequiredArgsConstructor
@Tag(name = "Product Variants", description = "Variants of a catalog product")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF', 'VENDOR')")
    @Operation(summary = "List a product's variants")
    public ResponseEntity<ApiResponse<List<VariantResponse>>> list(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(variantService.list(productId, principal.getTenantId())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Add a variant to a product")
    public ResponseEntity<ApiResponse<VariantResponse>> create(
            @PathVariable UUID productId,
            @Valid @RequestBody VariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VariantResponse response = variantService.create(productId, principal.getTenantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Update a product variant")
    public ResponseEntity<ApiResponse<VariantResponse>> update(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody VariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                variantService.update(productId, variantId, principal.getTenantId(), request)));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Delete a product variant")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        variantService.delete(productId, variantId, principal.getTenantId());
        return ResponseEntity.ok(ApiResponse.message("Variant deleted"));
    }
}
