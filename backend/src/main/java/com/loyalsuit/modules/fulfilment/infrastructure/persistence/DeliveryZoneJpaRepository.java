package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DeliveryZoneJpaRepository extends JpaRepository<DeliveryZone, UUID> {
    Optional<DeliveryZone> findByIdAndTenantId(UUID id, UUID tenantId);
    List<DeliveryZone> findByPickupPointIdAndTenantIdOrderByName(UUID pickupPointId, UUID tenantId);
    List<DeliveryZone> findByTenantId(UUID tenantId);
    List<DeliveryZone> findByTenantIdAndActiveTrue(UUID tenantId);
}
