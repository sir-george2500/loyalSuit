package com.loyalsuit.modules.loyalty.domain.port;

import com.loyalsuit.modules.loyalty.domain.LoyaltyTransaction;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface LoyaltyTransactionRepository {
    LoyaltyTransaction save(LoyaltyTransaction transaction);
    boolean existsByOrderIdAndType(UUID orderId, LoyaltyTransactionType type);
    Optional<LoyaltyTransaction> findByOrderIdAndType(UUID orderId, LoyaltyTransactionType type);
    Page<LoyaltyTransaction> findByTenantIdAndAccountId(UUID tenantId, UUID accountId, Pageable pageable);
}
