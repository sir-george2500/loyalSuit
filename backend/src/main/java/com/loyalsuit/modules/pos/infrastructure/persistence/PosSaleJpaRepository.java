package com.loyalsuit.modules.pos.infrastructure.persistence;

import com.loyalsuit.modules.pos.domain.PosSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

interface PosSaleJpaRepository extends JpaRepository<PosSale, UUID> {
    Optional<PosSale> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<PosSale> findByTenantIdAndClientSaleId(UUID tenantId, String clientSaleId);
    Page<PosSale> findByTenantId(UUID tenantId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM PosSale s WHERE s.tenantId = :tenantId AND s.shiftId = :shiftId")
    BigDecimal sumTotalByShift(@Param("tenantId") UUID tenantId, @Param("shiftId") UUID shiftId);

    long countByTenantIdAndShiftId(UUID tenantId, UUID shiftId);
}
