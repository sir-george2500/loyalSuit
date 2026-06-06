package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.CommissionEntry;
import com.loyalsuit.modules.marketplace.domain.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

interface CommissionEntryJpaRepository extends JpaRepository<CommissionEntry, UUID> {
    List<CommissionEntry> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
    Page<CommissionEntry> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<CommissionEntry> findByTenantIdAndVendorIdOrderByCreatedAtDesc(
            UUID tenantId, UUID vendorId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(c.netAmount), 0)
            FROM CommissionEntry c
            WHERE c.tenantId = :tenantId AND c.vendorId = :vendorId AND c.status = :status
            """)
    BigDecimal sumNetAmount(UUID tenantId, UUID vendorId, CommissionStatus status);
}
