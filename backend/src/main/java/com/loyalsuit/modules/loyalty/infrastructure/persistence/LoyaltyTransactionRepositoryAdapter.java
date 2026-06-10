package com.loyalsuit.modules.loyalty.infrastructure.persistence;

import com.loyalsuit.modules.loyalty.domain.LoyaltyTransaction;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransactionType;
import com.loyalsuit.modules.loyalty.domain.port.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LoyaltyTransactionRepositoryAdapter implements LoyaltyTransactionRepository {

    private final LoyaltyTransactionJpaRepository jpa;

    @Override
    public LoyaltyTransaction save(LoyaltyTransaction transaction) {
        return jpa.save(transaction);
    }

    @Override
    public boolean existsByOrderIdAndType(UUID orderId, LoyaltyTransactionType type) {
        return jpa.existsByOrderIdAndType(orderId, type);
    }

    @Override
    public Optional<LoyaltyTransaction> findByOrderIdAndType(UUID orderId, LoyaltyTransactionType type) {
        return jpa.findByOrderIdAndType(orderId, type);
    }

    @Override
    public Page<LoyaltyTransaction> findByTenantIdAndAccountId(UUID tenantId, UUID accountId, Pageable pageable) {
        return jpa.findByTenantIdAndAccountIdOrderByCreatedAtDesc(tenantId, accountId, pageable);
    }
}
