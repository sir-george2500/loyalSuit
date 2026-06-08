package com.loyalsuit.modules.pos.infrastructure.persistence;

import com.loyalsuit.modules.pos.domain.PosShift;
import com.loyalsuit.modules.pos.domain.PosShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PosShiftJpaRepository extends JpaRepository<PosShift, UUID> {
    Optional<PosShift> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<PosShift> findByTenantIdAndCashierIdAndStatus(UUID tenantId, UUID cashierId, PosShiftStatus status);
    Page<PosShift> findByTenantId(UUID tenantId, Pageable pageable);
}
