package com.loyalsuit.modules.orders.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.orders.application.CheckoutService;
import com.loyalsuit.modules.orders.application.dto.CheckoutRequest;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import com.loyalsuit.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Public cash checkout. Guests order without an account; an authenticated customer
 * of the store is linked to the order. The cart is identified by {@code X-Cart-Token}
 * and a double-submit is made safe by an optional {@code Idempotency-Key}.
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Place a cash order")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    @Operation(summary = "Place a cash order from the cart")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @PathVariable String storeSlug,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CheckoutRequest request) {
        if (!StringUtils.hasText(cartToken)) {
            throw new BusinessException("Missing cart token");
        }
        UUID authUserId = principal != null ? principal.getUserId() : null;
        UUID authTenantId = principal != null ? principal.getTenantId() : null;

        OrderResponse order = checkoutService.checkout(
                storeSlug, cartToken, request, idempotencyKey, authUserId, authTenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }
}
