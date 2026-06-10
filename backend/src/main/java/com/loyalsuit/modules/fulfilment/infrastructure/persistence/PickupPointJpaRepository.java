package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.PickupPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PickupPointJpaRepository extends JpaRepository<PickupPoint, UUID> {
    Optional<PickupPoint> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<PickupPoint> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<PickupPoint> findByTenantIdAndActiveTrueOrderByName(UUID tenantId);
}
