package com.loyalsuit.modules.orders.infrastructure.persistence;

import com.loyalsuit.modules.orders.domain.ReturnRequest;
import com.loyalsuit.modules.orders.domain.ReturnStatus;
import com.loyalsuit.modules.orders.domain.port.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReturnRequestRepositoryAdapter implements ReturnRequestRepository {

    private final ReturnRequestJpaRepository jpa;

    @Override
    public ReturnRequest save(ReturnRequest request) {
        return jpa.save(request);
    }

    @Override
    public Optional<ReturnRequest> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public boolean existsByOrderIdAndStatus(UUID orderId, ReturnStatus status) {
        return jpa.existsByOrderIdAndStatus(orderId, status);
    }

    @Override
    public Page<ReturnRequest> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<ReturnRequest> findByTenantIdAndStatus(UUID tenantId, ReturnStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }
}
