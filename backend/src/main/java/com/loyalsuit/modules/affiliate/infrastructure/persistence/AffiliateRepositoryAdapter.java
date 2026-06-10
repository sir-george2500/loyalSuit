package com.loyalsuit.modules.affiliate.infrastructure.persistence;

import com.loyalsuit.modules.affiliate.domain.Affiliate;
import com.loyalsuit.modules.affiliate.domain.port.AffiliateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AffiliateRepositoryAdapter implements AffiliateRepository {

    private final AffiliateJpaRepository jpa;

    @Override
    public Affiliate save(Affiliate affiliate) {
        return jpa.save(affiliate);
    }

    @Override
    public Optional<Affiliate> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }

    @Override
    public Optional<Affiliate> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public Optional<Affiliate> findByTenantIdAndCode(UUID tenantId, String code) {
        return jpa.findByTenantIdAndCode(tenantId, code);
    }

    @Override
    public boolean existsByTenantIdAndCode(UUID tenantId, String code) {
        return jpa.existsByTenantIdAndCode(tenantId, code);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpa.existsByUserId(userId);
    }

    @Override
    public Page<Affiliate> findByTenantId(UUID tenantId, Pageable pageable) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }
}
