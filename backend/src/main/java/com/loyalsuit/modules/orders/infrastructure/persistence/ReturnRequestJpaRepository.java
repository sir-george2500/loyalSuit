package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.ReturnRequest;
import com.loyalsuit.modules.orders.domain.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ReturnRequestJpaRepository extends JpaRepository<ReturnRequest, UUID> {
    Optional<ReturnRequest> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByOrderIdAndStatus(UUID orderId, ReturnStatus status);
    Page<ReturnRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<ReturnRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, ReturnStatus status, Pageable pageable);
}
