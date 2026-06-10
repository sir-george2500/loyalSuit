package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.common.response.ApiResponse;
import com.loyalsuit.modules.fulfilment.application.PickupPointService;
import com.loyalsuit.modules.fulfilment.application.dto.PickupPointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public storefront read of a store's pickup points and their zones, used at checkout to
 * pick a point + zone and price shipping. Lives under {@code /store}, permitted for GET.
 */
@RestController
@RequestMapping("/api/v1/store/{storeSlug}/pickup-points")
@RequiredArgsConstructor
@Tag(name = "Pickup points (storefront)", description = "Public pickup points & zones")
public class StorePickupPointController {

    private final PickupPointService pickupPointService;

    @GetMapping
    @Operation(summary = "Active pickup points and their delivery zones")
    public ResponseEntity<ApiResponse<List<PickupPointResponse>>> list(@PathVariable String storeSlug) {
        return ResponseEntity.ok(ApiResponse.ok(pickupPointService.publicPoints(storeSlug)));
    }
}
