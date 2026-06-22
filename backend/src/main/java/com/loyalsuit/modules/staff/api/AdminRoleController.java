package com.loyalsuit.modules.staff.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.staff.application.RolePermissionCatalog;
import com.loyalsuit.modules.staff.application.dto.RolePermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only Roles & Permissions matrix (the role model is fixed in code). */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Tag(name = "Roles & Permissions", description = "Role capability matrix")
public class AdminRoleController {

    private final RolePermissionCatalog catalog;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List roles and their permissions")
    public ResponseEntity<ApiResponse<List<RolePermissions>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(catalog.all()));
    }
}
