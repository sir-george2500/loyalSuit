package com.loyalsuit.modules.orders.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.orders.application.OrderTrackingService;
import com.loyalsuit.modules.orders.application.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public order tracking. A guest looks up their order with the order number plus the
 * email they used at checkout; a wrong email (or unknown number) is a 404, so order
 * numbers can't be enumerated. Lives under /store, which is permitted for GET.
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/orders")
@RequiredArgsConstructor
@Tag(name = "Order tracking", description = "Public guest order lookup")
public class StoreOrderController {

    private final OrderTrackingService orderTrackingService;

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Track an order by number + the email used at checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> track(
            @PathVariable String storeSlug,
            @PathVariable String orderNumber,
            @RequestParam String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Email is required to look up an order");
        }
        return ResponseEntity.ok(ApiResponse.ok(orderTrackingService.track(storeSlug, orderNumber, email)));
    }
}
