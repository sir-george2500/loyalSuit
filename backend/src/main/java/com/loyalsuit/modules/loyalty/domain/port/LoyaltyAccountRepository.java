package com.loyalsuit.modules.loyalty.domain.port;

import com.loyalsuit.modules.loyalty.domain.LoyaltyAccount;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltyAccountRepository {
    LoyaltyAccount save(LoyaltyAccount account);
    Optional<LoyaltyAccount> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Optional<LoyaltyAccount> findByIdAndTenantId(UUID id, UUID tenantId);
}
