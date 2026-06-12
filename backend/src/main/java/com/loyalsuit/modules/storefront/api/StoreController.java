package com.loyalsuit.modules.storefront.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.storefront.application.StorefrontService;
import com.loyalsuit.modules.storefront.application.dto.StoreCategory;
import com.loyalsuit.modules.storefront.application.dto.StoreProductDetail;
import com.loyalsuit.modules.storefront.application.dto.MarketplaceProductResult;
import com.loyalsuit.modules.storefront.application.dto.StoreProductSummary;
import com.loyalsuit.modules.storefront.application.dto.StoreSummary;
import com.loyalsuit.modules.storefront.application.dto.StoreView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, anonymous storefront read API. The store is resolved from the slug in the
 * path (not a JWT). Only GET is exposed here, and SecurityConfig permits
 * {@code GET /api/v1/store/**} without authentication.
 */
@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
@Tag(name = "Storefront", description = "Public, anonymous catalog browsing")
public class StoreController {

    private final StorefrontService storefrontService;

    @GetMapping
    @Operation(summary = "List all browsable stores (the public marketplace index)")
    public ResponseEntity<ApiResponse<List<StoreSummary>>> stores() {
        return ResponseEntity.ok(ApiResponse.ok(storefrontService.listStores()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products across all stores")
    public ResponseEntity<ApiResponse<PageResponse<MarketplaceProductResult>>> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(storefrontService.search(q, pageable)));
    }

    @GetMapping("/{storeSlug}")
    @Operation(summary = "Get a store's public profile")
    public ResponseEntity<ApiResponse<StoreView>> store(@PathVariable String storeSlug) {
        return ResponseEntity.ok(ApiResponse.ok(storefrontService.getStore(storeSlug)));
    }

    @GetMapping("/{storeSlug}/categories")
    @Operation(summary = "List a store's active categories")
    public ResponseEntity<ApiResponse<List<StoreCategory>>> categories(@PathVariable String storeSlug) {
        return ResponseEntity.ok(ApiResponse.ok(storefrontService.categories(storeSlug)));
    }

    @GetMapping("/{storeSlug}/products")
    @Operation(summary = "Browse a store's published products")
    public ResponseEntity<ApiResponse<PageResponse<StoreProductSummary>>> products(
            @PathVariable String storeSlug,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 24) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(storefrontService.products(storeSlug, category, pageable)));
    }

    @GetMapping("/{storeSlug}/products/{productSlug}")
    @Operation(summary = "Get a published product's detail page")
    public ResponseEntity<ApiResponse<StoreProductDetail>> product(
            @PathVariable String storeSlug,
            @PathVariable String productSlug) {
        return ResponseEntity.ok(ApiResponse.ok(storefrontService.product(storeSlug, productSlug)));
    }
}
