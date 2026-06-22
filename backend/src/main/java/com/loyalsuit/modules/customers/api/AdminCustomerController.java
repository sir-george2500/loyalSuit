package com.loyalsuit.modules.customers.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.customers.application.CustomerService;
import com.loyalsuit.modules.customers.application.dto.CustomerResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin view of a tenant's storefront customers. Read-only; visible to store staff. */
@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Admin customer directory")
public class AdminCustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "List customers (optionally filtered by name/email)")
    public ResponseEntity<ApiResponse<PageResponse<CustomerResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.list(principal.getTenantId(), search, pageable)));
    }
}
