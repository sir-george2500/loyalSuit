package com.loyalsuit.modules.featureflags.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.featureflags.application.FeatureFlagService;
import com.loyalsuit.modules.featureflags.application.dto.FeatureFlagResponse;
import com.loyalsuit.modules.featureflags.application.dto.UpsertFeatureFlagRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Feature flag management. Platform owner only. */
@RestController
@RequestMapping("/api/v1/platform/flags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Feature flags", description = "Platform feature flags")
public class PlatformFeatureFlagController {

    private final FeatureFlagService service;

    @GetMapping
    @Operation(summary = "List feature flags")
    public ResponseEntity<ApiResponse<List<FeatureFlagResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @PostMapping
    @Operation(summary = "Create a feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> create(
            @Valid @RequestBody UpsertFeatureFlagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpsertFeatureFlagRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable a feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> enable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setEnabled(id, true)));
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable a feature flag")
    public ResponseEntity<ApiResponse<FeatureFlagResponse>> disable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setEnabled(id, false)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a feature flag")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Feature flag deleted"));
    }
}
