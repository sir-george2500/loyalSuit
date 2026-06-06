package com.loyalsuit.modules.orders.domain.port;

import com.loyalsuit.modules.orders.domain.ReturnRequest;
import com.loyalsuit.modules.orders.domain.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ReturnRequestRepository {
    ReturnRequest save(ReturnRequest request);
    Optional<ReturnRequest> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByOrderIdAndStatus(UUID orderId, ReturnStatus status);
    Page<ReturnRequest> findByTenantId(UUID tenantId, Pageable pageable);
    Page<ReturnRequest> findByTenantIdAndStatus(UUID tenantId, ReturnStatus status, Pageable pageable);
}
