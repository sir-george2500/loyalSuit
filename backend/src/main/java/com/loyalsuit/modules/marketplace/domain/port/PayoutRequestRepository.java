package com.loyalsuit.modules.marketplace.domain.port;

import com.loyalsuit.modules.marketplace.domain.PayoutRequest;
import com.loyalsuit.modules.marketplace.domain.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRequestRepository {
    PayoutRequest save(PayoutRequest request);
    Optional<PayoutRequest> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndVendorIdAndStatus(UUID tenantId, UUID vendorId, PayoutStatus status);
    Page<PayoutRequest> findByTenantId(UUID tenantId, Pageable pageable);
    Page<PayoutRequest> findByTenantIdAndStatus(UUID tenantId, PayoutStatus status, Pageable pageable);
    Page<PayoutRequest> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable);

    /** Total payout amount for a vendor in a given status (0 when none); for balances. */
    BigDecimal sumAmount(UUID tenantId, UUID vendorId, PayoutStatus status);
}
