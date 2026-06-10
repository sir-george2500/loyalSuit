package com.loyalsuit.modules.loyalty.infrastructure.persistence;

import com.loyalsuit.modules.loyalty.domain.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface LoyaltyAccountJpaRepository extends JpaRepository<LoyaltyAccount, UUID> {
    Optional<LoyaltyAccount> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Optional<LoyaltyAccount> findByIdAndTenantId(UUID id, UUID tenantId);
}
