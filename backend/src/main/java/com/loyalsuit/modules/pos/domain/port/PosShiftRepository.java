package com.loyalsuit.modules.pos.domain.port;

import com.loyalsuit.modules.pos.domain.PosShift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PosShiftRepository {
    PosShift save(PosShift shift);
    Optional<PosShift> findByIdAndTenantId(UUID id, UUID tenantId);
    /** The cashier's currently-open drawer, if any (at most one by the unique index). */
    Optional<PosShift> findOpenByCashier(UUID tenantId, UUID cashierId);
    Page<PosShift> findByTenantId(UUID tenantId, Pageable pageable);
}
