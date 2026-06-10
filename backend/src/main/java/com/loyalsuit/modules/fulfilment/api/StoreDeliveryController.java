package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.fulfilment.application.DeliveryTrackingService;
import com.loyalsuit.modules.fulfilment.application.dto.DeliveryTrackingResponse;
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
 * Public delivery tracking for a guest. Like order tracking, it requires the order number
 * plus the email used at checkout (a mismatch is the same generic 404). Lives under
 * {@code /store}, which is permitted for GET; the response is sanitized for the customer.
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/orders/{orderNumber}/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery tracking", description = "Public guest delivery tracking")
public class StoreDeliveryController {

    private final DeliveryTrackingService deliveryTrackingService;

    @GetMapping
    @Operation(summary = "Track a delivery by order number + the email used at checkout")
    public ResponseEntity<ApiResponse<DeliveryTrackingResponse>> track(
            @PathVariable String storeSlug,
            @PathVariable String orderNumber,
            @RequestParam String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("Email is required to track a delivery");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                deliveryTrackingService.track(storeSlug, orderNumber, email)));
    }
}
