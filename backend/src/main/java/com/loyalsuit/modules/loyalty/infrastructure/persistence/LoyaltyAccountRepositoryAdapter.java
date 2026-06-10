package com.loyalsuit.modules.loyalty.infrastructure.persistence;

import com.loyalsuit.modules.loyalty.domain.LoyaltyAccount;
import com.loyalsuit.modules.loyalty.domain.port.LoyaltyAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LoyaltyAccountRepositoryAdapter implements LoyaltyAccountRepository {

    private final LoyaltyAccountJpaRepository jpa;

    @Override
    public LoyaltyAccount save(LoyaltyAccount account) {
        return jpa.save(account);
    }

    @Override
    public Optional<LoyaltyAccount> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return jpa.findByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Override
    public Optional<LoyaltyAccount> findByIdAndTenantId(UUID id, UUID tenantId) {
        return jpa.findByIdAndTenantId(id, tenantId);
    }
}
