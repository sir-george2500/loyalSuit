package com.loyalsuit.modules.loyalty.application;

import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.loyalty.application.dto.LoyaltyBalanceResponse;
import com.loyalsuit.modules.loyalty.application.dto.LoyaltyTransactionResponse;
import com.loyalsuit.modules.loyalty.domain.LoyaltyAccount;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransaction;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransactionType;
import com.loyalsuit.modules.loyalty.domain.port.LoyaltyAccountRepository;
import com.loyalsuit.modules.loyalty.domain.port.LoyaltyTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * The loyalty-points engine. A registered customer earns points when their order is paid
 * (and loses them if it's refunded); points are worth a fixed value as a discount, which
 * checkout redemption (8d.2) draws against. Every movement updates the account balance and
 * appends to the ledger; accrual and reversal are idempotent on the order.
 *
 * <p>Economics are fixed for now: {@value #POINTS_PER_UNIT} point per whole currency unit
 * paid, each point worth {@code POINT_VALUE}. (Tenant-configurable rates can come later.)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoyaltyService {

    public static final int POINTS_PER_UNIT = 1;
    /** What one point is worth as a discount (100 points = $1). */
    public static final BigDecimal POINT_VALUE = new BigDecimal("0.01");

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;

    /** Credit points for a just-paid order (idempotent per order; a no-op without a customer). */
    @Transactional
    public void earnForOrder(UUID tenantId, UUID customerId, UUID orderId, BigDecimal amountPaid) {
        if (customerId == null || amountPaid == null) {
            return;
        }
        int points = amountPaid.setScale(0, RoundingMode.FLOOR).intValue() * POINTS_PER_UNIT;
        if (points <= 0 || transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.EARN)) {
            return;
        }
        LoyaltyAccount account = accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .orElseGet(() -> new LoyaltyAccount(tenantId, customerId));
        account.credit(points);
        LoyaltyAccount saved = accountRepository.save(account);
        transactionRepository.save(
                new LoyaltyTransaction(tenantId, saved.getId(), orderId, LoyaltyTransactionType.EARN, points));
    }

    /** Claw back the points an order earned, if it's refunded (idempotent; never below zero). */
    @Transactional
    public void reverseForOrder(UUID tenantId, UUID orderId) {
        LoyaltyTransaction earned = transactionRepository
                .findByOrderIdAndType(orderId, LoyaltyTransactionType.EARN).orElse(null);
        if (earned == null || transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.REVERSE)) {
            return;
        }
        LoyaltyAccount account = accountRepository.findByIdAndTenantId(earned.getAccountId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Loyalty account", earned.getAccountId()));
        int removed = account.debit(earned.getPoints());
        accountRepository.save(account);
        transactionRepository.save(
                new LoyaltyTransaction(tenantId, account.getId(), orderId, LoyaltyTransactionType.REVERSE, -removed));
    }

    public LoyaltyBalanceResponse balanceFor(UUID tenantId, UUID customerId) {
        int points = accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .map(LoyaltyAccount::getPoints)
                .orElse(0);
        return new LoyaltyBalanceResponse(points, valueOf(points));
    }

    public PageResponse<LoyaltyTransactionResponse> history(UUID tenantId, UUID customerId, Pageable pageable) {
        return accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .map(account -> new PageResponse<>(transactionRepository
                        .findByTenantIdAndAccountId(tenantId, account.getId(), pageable)
                        .map(LoyaltyTransactionResponse::from)))
                .orElseGet(() -> new PageResponse<>(Page.<LoyaltyTransactionResponse>empty(pageable)));
    }

    /** The discount value of a number of points (points × point value, to the cent). */
    public static BigDecimal valueOf(int points) {
        return POINT_VALUE.multiply(BigDecimal.valueOf(points)).setScale(2, RoundingMode.HALF_UP);
    }
}
