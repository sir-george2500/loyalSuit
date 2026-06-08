package com.loyalsuit.modules.pos.infrastructure.persistence;

import com.loyalsuit.modules.pos.domain.PosShift;
import com.loyalsuit.modules.pos.domain.PosShiftStatus;
import com.loyalsuit.modules.pos.domain.port.PosShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PosShiftRepositoryAdapter implements PosShiftRepository {

    private final PosShiftJpaRepository jpa;

    @Override
    public PosShift save(PosShift shift) {
        return jpa.save(shift);
    }

    @Override
    public Optional<PosShift> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<PosShift> findOpenByCashier(UUID tenantId, UUID cashierId) {
        return jpa.findByTenantIdAndCashierIdAndStatus(tenantId, cashierId, PosShiftStatus.OPEN);
    }

    @Override
    public Page<PosShift> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantId(tenantId, pageable);
    }
}
