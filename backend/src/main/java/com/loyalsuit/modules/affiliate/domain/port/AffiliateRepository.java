package com.loyalsuit.modules.affiliate.domain.port;

import com.loyalsuit.modules.affiliate.domain.Affiliate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface AffiliateRepository {
    Affiliate save(Affiliate affiliate);
    Optional<Affiliate> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Affiliate> findByUserId(UUID userId);
    Optional<Affiliate> findByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByTenantIdAndCode(UUID tenantId, String code);
    boolean existsByUserId(UUID userId);
    Page<Affiliate> findByTenantId(UUID tenantId, Pageable pageable);
}
