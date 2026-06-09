package com.loyalsuit.modules.pos.infrastructure.persistence;

import com.loyalsuit.modules.pos.domain.PosSale;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class PosSaleRepositoryAdapter implements PosSaleRepository {

    private final PosSaleJpaRepository jpa;

    @Override
    public PosSale save(PosSale sale) {
        return jpa.save(sale);
    }

    @Override
    public Optional<PosSale> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<PosSale> findByTenantIdAndClientSaleId(UUID tenantId, String clientSaleId) {
        return jpa.findByTenantIdAndClientSaleId(tenantId, clientSaleId);
    }

    @Override
    public Page<PosSale> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantId(tenantId, pageable);
    }

    @Override
    public BigDecimal sumTotalByShift(UUID tenantId, UUID shiftId) {
        return jpa.sumTotalByShift(tenantId, shiftId);
    }

    @Override
    public BigDecimal sumCashCollectedByShift(UUID tenantId, UUID shiftId) {
        return jpa.sumCashCollectedByShift(tenantId, shiftId);
    }

    @Override
    public long countByShift(UUID tenantId, UUID shiftId) {
        return jpa.countByTenantIdAndShiftId(tenantId, shiftId);
    }
}
