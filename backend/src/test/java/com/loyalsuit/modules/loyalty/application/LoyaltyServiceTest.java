package com.loyalsuit.modules.loyalty.application;

import com.loyalsuit.modules.loyalty.application.dto.LoyaltyBalanceResponse;
import com.loyalsuit.modules.loyalty.domain.LoyaltyAccount;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransaction;
import com.loyalsuit.modules.loyalty.domain.LoyaltyTransactionType;
import com.loyalsuit.modules.loyalty.domain.port.LoyaltyAccountRepository;
import com.loyalsuit.modules.loyalty.domain.port.LoyaltyTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock private LoyaltyAccountRepository accountRepository;
    @Mock private LoyaltyTransactionRepository transactionRepository;

    @InjectMocks private LoyaltyService service;

    private UUID tenantId;
    private UUID customerId;
    private UUID orderId;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        accountId = UUID.randomUUID();
    }

    private LoyaltyAccount account(int points) {
        LoyaltyAccount account = new LoyaltyAccount(tenantId, customerId);
        account.setPoints(points);
        ReflectionTestUtils.setField(account, "id", accountId);
        return account;
    }

    // ---- earn ---------------------------------------------------------------

    @Test
    void earnForOrder_creditsFlooredPoints_andRecordsTheLedger() {
        // Arrange — paid 49.99 → 49 points; no account yet
        when(transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.EARN)).thenReturn(false);
        when(accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(Optional.empty());
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> {
            LoyaltyAccount a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", accountId);
            return a;
        });

        // Act
        service.earnForOrder(tenantId, customerId, orderId, new BigDecimal("49.99"));

        // Assert
        ArgumentCaptor<LoyaltyTransaction> tx = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo(LoyaltyTransactionType.EARN);
        assertThat(tx.getValue().getPoints()).isEqualTo(49);
    }

    @Test
    void earnForOrder_isIdempotentPerOrder() {
        // Arrange — the order already earned
        when(transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.EARN)).thenReturn(true);

        // Act
        service.earnForOrder(tenantId, customerId, orderId, new BigDecimal("49.99"));

        // Assert
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void earnForOrder_withoutACustomer_isANoOp() {
        // Act — guest order (no customer id)
        service.earnForOrder(tenantId, null, orderId, new BigDecimal("49.99"));

        // Assert
        verify(transactionRepository, never()).existsByOrderIdAndType(any(), any());
        verify(accountRepository, never()).save(any());
    }

    // ---- reverse ------------------------------------------------------------

    @Test
    void reverseForOrder_clawsBackThePointsTheOrderEarned() {
        // Arrange — order earned 49, balance is 49
        LoyaltyTransaction earned = new LoyaltyTransaction(tenantId, accountId, orderId, LoyaltyTransactionType.EARN, 49);
        when(transactionRepository.findByOrderIdAndType(orderId, LoyaltyTransactionType.EARN)).thenReturn(Optional.of(earned));
        when(transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.REVERSE)).thenReturn(false);
        LoyaltyAccount account = account(49);
        when(accountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.reverseForOrder(tenantId, orderId);

        // Assert — balance back to 0, a REVERSE of -49 recorded
        assertThat(account.getPoints()).isZero();
        ArgumentCaptor<LoyaltyTransaction> tx = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo(LoyaltyTransactionType.REVERSE);
        assertThat(tx.getValue().getPoints()).isEqualTo(-49);
    }

    @Test
    void reverseForOrder_whenTheOrderNeverEarned_isANoOp() {
        // Arrange
        when(transactionRepository.findByOrderIdAndType(orderId, LoyaltyTransactionType.EARN)).thenReturn(Optional.empty());

        // Act
        service.reverseForOrder(tenantId, orderId);

        // Assert
        verify(accountRepository, never()).save(any());
    }

    // ---- redeem -------------------------------------------------------------

    @Test
    void validateRedemption_returnsTheDiscount_whenTheBalanceCovers() {
        // Arrange — 250 points available, spending 100 → $1.00
        when(accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(Optional.of(account(250)));

        // Act & Assert
        assertThat(service.validateRedemption(tenantId, customerId, 100)).isEqualByComparingTo("1.00");
    }

    @Test
    void validateRedemption_rejects_whenTheBalanceIsTooLow() {
        // Arrange — only 50 points, trying to spend 100
        when(accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(Optional.of(account(50)));

        // Act & Assert
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.validateRedemption(tenantId, customerId, 100))
                .isInstanceOf(com.loyalsuit.common.exception.BusinessException.class)
                .hasMessageContaining("enough points");
    }

    @Test
    void redeemForOrder_debitsTheBalance_andRecordsTheSpend() {
        // Arrange — 250 points, spending 100
        LoyaltyAccount account = account(250);
        when(transactionRepository.existsByOrderIdAndType(orderId, LoyaltyTransactionType.REDEEM)).thenReturn(false);
        when(accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(LoyaltyAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.redeemForOrder(tenantId, customerId, orderId, 100);

        // Assert
        assertThat(account.getPoints()).isEqualTo(150);
        ArgumentCaptor<LoyaltyTransaction> tx = ArgumentCaptor.forClass(LoyaltyTransaction.class);
        verify(transactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo(LoyaltyTransactionType.REDEEM);
        assertThat(tx.getValue().getPoints()).isEqualTo(-100);
    }

    // ---- balance ------------------------------------------------------------

    @Test
    void balanceFor_returnsPoints_andTheirRedeemableValue() {
        // Arrange — 250 points → $2.50 at 100 points/$1
        when(accountRepository.findByTenantIdAndCustomerId(tenantId, customerId)).thenReturn(Optional.of(account(250)));

        // Act
        LoyaltyBalanceResponse balance = service.balanceFor(tenantId, customerId);

        // Assert
        assertThat(balance.points()).isEqualTo(250);
        assertThat(balance.redeemableValue()).isEqualByComparingTo("2.50");
    }
}
