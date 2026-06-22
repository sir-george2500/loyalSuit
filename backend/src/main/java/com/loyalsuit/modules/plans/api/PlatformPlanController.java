package com.loyalsuit.modules.plans.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.plans.application.PlanService;
import com.loyalsuit.modules.plans.application.dto.PlanResponse;
import com.loyalsuit.modules.plans.application.dto.UpsertPlanRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Subscription plan catalogue management. Platform owner only. */
@RestController
@RequestMapping("/api/v1/platform/plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Subscription plans", description = "Platform plan catalogue")
public class PlatformPlanController {

    private final PlanService service;

    @GetMapping
    @Operation(summary = "List plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @PostMapping
    @Operation(summary = "Create a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody UpsertPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(service.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpsertPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setActive(id, true)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a plan")
    public ResponseEntity<ApiResponse<PlanResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.setActive(id, false)));
    }
}
