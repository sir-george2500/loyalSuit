package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.PickupPoint;
import com.loyalsuit.modules.fulfilment.domain.port.PickupPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PickupPointRepositoryAdapter implements PickupPointRepository {

    private final PickupPointJpaRepository jpa;

    @Override
    public PickupPoint save(PickupPoint point) {
        return jpa.save(point);
    }

    @Override
    public Optional<PickupPoint> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Page<PickupPoint> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public List<PickupPoint> findActiveByTenantId(UUID tenantId) {
        return jpa.findByTenantIdAndActiveTrueOrderByName(tenantId);
    }
}
