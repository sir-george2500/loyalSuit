package com.loyalsuit.modules.marketplace.domain.port;

import com.loyalsuit.modules.marketplace.domain.CommissionEntry;
import com.loyalsuit.modules.marketplace.domain.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CommissionEntryRepository {
    List<CommissionEntry> saveAll(List<CommissionEntry> entries);
    List<CommissionEntry> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
    Page<CommissionEntry> findByTenantId(UUID tenantId, Pageable pageable);
    Page<CommissionEntry> findByTenantIdAndVendorId(UUID tenantId, UUID vendorId, Pageable pageable);

    /** Sum of net amounts for a vendor in a given status (0 when none); for balances. */
    BigDecimal sumNetAmount(UUID tenantId, UUID vendorId, CommissionStatus status);
}
