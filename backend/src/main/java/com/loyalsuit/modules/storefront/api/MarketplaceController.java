package com.loyalsuit.modules.storefront.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.storefront.application.MarketplaceService;
import com.loyalsuit.modules.storefront.application.dto.MarketplaceProductCard;
import com.loyalsuit.modules.storefront.application.dto.StoreCategory;
import com.loyalsuit.modules.storefront.application.dto.StoreView;
import com.loyalsuit.modules.storefront.application.dto.VendorStorefront;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, anonymous API for the LoyalSuit marketplace — the single flagship storefront. Resolves the
 * canonical store server-side (no slug in the path), so the frontend never has to know it.
 * SecurityConfig permits {@code GET /api/v1/marketplace/**} without authentication.
 */
@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Public LoyalSuit marketplace (the flagship storefront)")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping
    @Operation(summary = "Get the marketplace store profile")
    public ResponseEntity<ApiResponse<StoreView>> store() {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.store()));
    }

    @GetMapping("/categories")
    @Operation(summary = "List the marketplace's active categories")
    public ResponseEntity<ApiResponse<List<StoreCategory>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.categories()));
    }

    @GetMapping("/products")
    @Operation(summary = "Browse the marketplace catalogue (house-first), optionally by category")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceProductCard>>> products(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.products(category, pageable)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search the marketplace catalogue by product name")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceProductCard>>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.search(q, pageable)));
    }

    @GetMapping("/vendors/{vendorSlug}")
    @Operation(summary = "Get a vendor's public storefront profile")
    public ResponseEntity<ApiResponse<VendorStorefront>> vendor(@PathVariable String vendorSlug) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.vendor(vendorSlug)));
    }

    @GetMapping("/vendors/{vendorSlug}/products")
    @Operation(summary = "Browse a vendor's products")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceProductCard>>> vendorProducts(
            @PathVariable String vendorSlug,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.vendorProducts(vendorSlug, pageable)));
    }
}
