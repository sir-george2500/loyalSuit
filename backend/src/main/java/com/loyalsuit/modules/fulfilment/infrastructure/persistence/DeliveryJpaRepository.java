package com.loyalsuit.modules.fulfilment.infrastructure.persistence;

import com.loyalsuit.modules.fulfilment.domain.Delivery;
import com.loyalsuit.modules.fulfilment.domain.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
    Optional<Delivery> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Delivery> findByOrderIdAndTenantId(UUID orderId, UUID tenantId);
    Optional<Delivery> findByOrderId(UUID orderId);
    Page<Delivery> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Delivery> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, DeliveryStatus status, Pageable pageable);
    Page<Delivery> findByTenantIdAndAgentIdAndStatusInOrderByCreatedAtDesc(
            UUID tenantId, UUID agentId, Collection<DeliveryStatus> statuses, Pageable pageable);
}
