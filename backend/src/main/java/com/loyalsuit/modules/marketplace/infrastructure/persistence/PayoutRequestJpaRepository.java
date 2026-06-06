package com.loyalsuit.modules.marketplace.infrastructure.persistence;

import com.loyalsuit.modules.marketplace.domain.PayoutRequest;
import com.loyalsuit.modules.marketplace.domain.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

interface PayoutRequestJpaRepository extends JpaRepository<PayoutRequest, UUID> {
    Optional<PayoutRequest> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndVendorIdAndStatus(UUID tenantId, UUID vendorId, PayoutStatus status);
    Page<PayoutRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<PayoutRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, PayoutStatus status, Pageable pageable);
    Page<PayoutRequest> findByTenantIdAndVendorIdOrderByCreatedAtDesc(
            UUID tenantId, UUID vendorId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM PayoutRequest p
            WHERE p.tenantId = :tenantId AND p.vendorId = :vendorId AND p.status = :status
            """)
    BigDecimal sumAmount(UUID tenantId, UUID vendorId, PayoutStatus status);
}
