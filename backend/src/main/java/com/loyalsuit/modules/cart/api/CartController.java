package com.loyalsuit.modules.cart.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.cart.application.CartService;
import com.loyalsuit.modules.cart.application.dto.AddItemRequest;
import com.loyalsuit.modules.cart.application.dto.CartView;
import com.loyalsuit.modules.cart.application.dto.UpdateItemRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public, anonymous shopping cart. The store is resolved from the slug and the cart
 * from an opaque client token carried in the {@code X-Cart-Token} header — so the
 * same endpoints serve guests and signed-in shoppers.
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Public shopping cart")
public class CartController {

    private static final String TOKEN_HEADER = "X-Cart-Token";

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "View the cart")
    public ResponseEntity<ApiResponse<CartView>> view(
            @PathVariable String storeSlug,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.view(storeSlug, requireToken(token))));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<ApiResponse<CartView>> addItem(
            @PathVariable String storeSlug,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.addItem(storeSlug, requireToken(token), request)));
    }

    @PutMapping("/items")
    @Operation(summary = "Set an item's quantity (0 removes it)")
    public ResponseEntity<ApiResponse<CartView>> updateItem(
            @PathVariable String storeSlug,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody UpdateItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateItem(storeSlug, requireToken(token), request)));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Remove an item from the cart")
    public ResponseEntity<ApiResponse<CartView>> removeItem(
            @PathVariable String storeSlug,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID variantId) {
        return ResponseEntity.ok(ApiResponse.ok(
                cartService.removeItem(storeSlug, requireToken(token), productId, variantId)));
    }

    @DeleteMapping
    @Operation(summary = "Empty the cart")
    public ResponseEntity<ApiResponse<Void>> clear(
            @PathVariable String storeSlug,
            @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        cartService.clear(storeSlug, requireToken(token));
        return ResponseEntity.ok(ApiResponse.message("Cart emptied"));
    }

    private static String requireToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException("Missing cart token");
        }
        return token;
    }
}
