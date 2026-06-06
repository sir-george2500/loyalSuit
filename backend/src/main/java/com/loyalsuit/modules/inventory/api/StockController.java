package com.loyalsuit.modules.inventory.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.inventory.application.StockService;
import com.loyalsuit.modules.inventory.application.dto.AdjustStockRequest;
import com.loyalsuit.modules.inventory.application.dto.SetStockRequest;
import com.loyalsuit.modules.inventory.application.dto.StockResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/stock")
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Per-warehouse stock levels")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "List stock rows for a product across warehouses")
    public ResponseEntity<ApiResponse<List<StockResponse>>> listForProduct(
            @RequestParam UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.listForProduct(productId, principal.getTenantId())));
    }

    @GetMapping("/low")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "List the tenant's low-stock rows")
    public ResponseEntity<ApiResponse<List<StockResponse>>> lowStock(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.lowStock(principal.getTenantId())));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Set the absolute stock level for a product/variant/warehouse")
    public ResponseEntity<ApiResponse<StockResponse>> setLevel(
            @Valid @RequestBody SetStockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.setLevel(request, principal.getTenantId())));
    }

    @PostMapping("/{stockId}/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF')")
    @Operation(summary = "Apply a signed delta to a stock row (atomic, never negative)")
    public ResponseEntity<ApiResponse<StockResponse>> adjust(
            @PathVariable UUID stockId,
            @Valid @RequestBody AdjustStockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                stockService.adjust(stockId, request.getDelta(), principal.getTenantId())));
    }
}
