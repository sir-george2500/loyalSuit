package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;
import com.loyalsuit.modules.fulfilment.domain.port.DeliveryZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeliveryZoneRepositoryAdapter implements DeliveryZoneRepository {

    private final DeliveryZoneJpaRepository jpa;

    @Override
    public DeliveryZone save(DeliveryZone zone) {
        return jpa.save(zone);
    }

    @Override
    public Optional<DeliveryZone> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public void delete(DeliveryZone zone) {
        jpa.delete(zone);
    }

    @Override
    public List<DeliveryZone> findByPickupPointIdAndTenantId(UUID pickupPointId, UUID tenantId) {
        return jpa.findByPickupPointIdAndTenantIdOrderByName(pickupPointId, tenantId);
    }

    @Override
    public List<DeliveryZone> findByTenantId(UUID tenantId) {
        return jpa.findByTenantId(tenantId);
    }

    @Override
    public List<DeliveryZone> findActiveByTenantId(UUID tenantId) {
        return jpa.findByTenantIdAndActiveTrue(tenantId);
    }
}
