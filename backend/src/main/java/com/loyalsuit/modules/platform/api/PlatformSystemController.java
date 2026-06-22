package com.loyalsuit.modules.platform.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.platform.application.SystemHealthService;
import com.loyalsuit.modules.platform.application.dto.SystemHealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** System health snapshot. Platform owner only. */
@RestController
@RequestMapping("/api/v1/platform/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "System health", description = "Platform health snapshot")
public class PlatformSystemController {

    private final SystemHealthService service;

    @GetMapping
    @Operation(summary = "Get a system health snapshot")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> health() {
        return ResponseEntity.ok(ApiResponse.ok(service.snapshot()));
    }
}
