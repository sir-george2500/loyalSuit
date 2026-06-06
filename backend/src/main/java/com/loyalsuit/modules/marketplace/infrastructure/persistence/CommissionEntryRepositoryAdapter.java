package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.CommissionEntry;
import com.loyalsuit.modules.marketplace.domain.CommissionStatus;
import com.loyalsuit.modules.marketplace.domain.port.CommissionEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CommissionEntryRepositoryAdapter implements CommissionEntryRepository {

    private final CommissionEntryJpaRepository jpa;

    @Override
    public List<CommissionEntry> saveAll(List<CommissionEntry> entries) {
        return jpa.saveAll(entries);
    }

    @Override
    public List<CommissionEntry> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId);
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return jpa.existsByOrderId(orderId);
    }

    @Override
    public Page<CommissionEntry> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Override
    public Page<CommissionEntry> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable) {
        return jpa.findByTenantIdAndVendorIdOrderByCreatedAtDesc(tenantId, vendorId, pageable);
    }

    @Override
    public BigDecimal sumNetAmount(UUID tenantId, UUID vendorId, CommissionStatus status) {
        return jpa.sumNetAmount(tenantId, vendorId, status);
    }
}
