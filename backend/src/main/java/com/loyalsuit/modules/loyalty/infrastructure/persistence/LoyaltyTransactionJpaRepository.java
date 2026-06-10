package com.loyalsuit.modules.loyalty.infrastructure.persistence;

import com.loyalsuit.modules.loyalty.domain.LoyaltyTransaction;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface LoyaltyTransactionJpaRepository extends JpaRepository<LoyaltyTransaction, UUID> {
    boolean existsByOrderIdAndType(UUID orderId, LoyaltyTransactionType type);
    Optional<LoyaltyTransaction> findByOrderIdAndType(UUID orderId, LoyaltyTransactionType type);
    Page<LoyaltyTransaction> findByTenantIdAndAccountIdOrderByCreatedAtDesc(
            UUID tenantId, UUID accountId, Pageable pageable);
}
