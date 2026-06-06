package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.PayoutRequest;
import com.loyalsuit.modules.marketplace.domain.PayoutStatus;
import com.loyalsuit.modules.marketplace.domain.port.PayoutRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PayoutRequestRepositoryAdapter implements PayoutRequestRepository {

    private final PayoutRequestJpaRepository jpa;

    @Override
    public PayoutRequest save(PayoutRequest request) {
        return jpa.save(request);
    }

    @Override
    public Optional<PayoutRequest> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public boolean existsByTenantIdAndVendorIdAndStatus(UUID tenantId, UUID vendorId, PayoutStatus status) {
        return jpa.existsByTenantIdAndVendorIdAndStatus(tenantId, vendorId, status);
    }

    @Override
    public Page<PayoutRequest> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<PayoutRequest> findByTenantIdAndStatus(UUID tenantId, PayoutStatus status, Pageable pageable) {
        return jpa.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status, pageable);
    }

    @Override
    public Page<PayoutRequest> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable) {
        return jpa.findByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId, pageable);
    }

    @Override
    public BigDecimal sumAmount(UUID tenantId, UUID vendorId, PayoutStatus status) {
        return jpa.sumAmount(tenantId, vendorId, status);
    }
}
