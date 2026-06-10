package com.loyalsuit.modules.fulfilment.domain.port;

import com.loyalsuit.modules.fulfilment.domain.PickupPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupPointRepository {
    PickupPoint save(PickupPoint point);
    Optional<PickupPoint> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<PickupPoint> findByTenantId(UUID tenantId, Pageable pageable);
    /** Active points, for the storefront and checkout. */
    List<PickupPoint> findActiveByTenantId(UUID tenantId);
}
