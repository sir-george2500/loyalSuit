package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.DeliveryAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface DeliveryAgentJpaRepository extends JpaRepository<DeliveryAgent, UUID> {
    Optional<DeliveryAgent> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<DeliveryAgent> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    Page<DeliveryAgent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<DeliveryAgent> findByTenantIdAndActiveOrderByCreatedAtDesc(UUID tenantId, boolean active, Pageable pageable);
}
