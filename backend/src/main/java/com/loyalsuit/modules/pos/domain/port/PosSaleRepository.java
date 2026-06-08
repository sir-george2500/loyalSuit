package com.loyalsuit.modules.pos.domain.port;

import com.loyalsuit.modules.pos.domain.PosSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PosSaleRepository {
    PosSale save(PosSale sale);
    Optional<PosSale> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<PosSale> findByTenantIdAndClientSaleId(UUID tenantId, String clientSaleId);
    Page<PosSale> findByTenantId(UUID tenantId, Pageable pageable);
    /** Total cash taken on a shift (0 when none) — the basis for drawer reconciliation. */
    BigDecimal sumTotalByShift(UUID tenantId, UUID shiftId);
    long countByShift(UUID tenantId, UUID shiftId);
}
