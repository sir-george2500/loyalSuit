package com.loyalsuit.modules.orders.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.orders.application.ReturnService;
import com.loyalsuit.modules.orders.application.dto.CreateReturnRequest;
import com.loyalsuit.modules.orders.application.dto.ReturnResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public return requests. A guest requests a return for a delivered order, verified
 * by the order number and the email used at checkout (same no-leak rule as tracking).
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/orders/{orderNumber}/returns")
@RequiredArgsConstructor
@Tag(name = "Returns", description = "Public return requests")
public class StoreReturnController {

    private final ReturnService returnService;

    @PostMapping
    @Operation(summary = "Request a return for a delivered order")
    public ResponseEntity<ApiResponse<ReturnResponse>> requestReturn(
            @PathVariable String storeSlug,
            @PathVariable String orderNumber,
            @Valid @RequestBody CreateReturnRequest request) {
        ReturnResponse response = returnService.requestReturn(storeSlug, orderNumber, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }
}
