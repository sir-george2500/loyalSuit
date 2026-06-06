package com.loyalsuit.modules.marketplace.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.marketplace.application.dto.PayoutBalanceResponse;
import com.loyalsuit.modules.marketplace.application.dto.PayoutResponse;
import com.loyalsuit.modules.marketplace.application.dto.VendorEarningsResponse;
import com.loyalsuit.modules.marketplace.domain.PayoutRequest;
import com.loyalsuit.modules.marketplace.domain.PayoutStatus;
import com.loyalsuit.modules.marketplace.domain.port.PayoutRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock private PayoutRequestRepository payoutRepository;
    @Mock private CommissionService commissionService;
    @Mock private AuditService auditService;

    @InjectMocks private PayoutService service;

    private UUID tenantId;
    private UUID vendorId;
    private UUID adminId;
    private UUID payoutId;
    private AuditActor vendorActor;
    private AuditActor adminActor;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        payoutId = UUID.randomUUID();
        vendorActor = AuditActor.of(tenantId, vendorId, "vendor@test.dev", "VENDOR");
        adminActor = AuditActor.of(tenantId, adminId, "admin@test.dev", "TENANT_ADMIN");
    }

    /** earned net = the vendor's EARNED commission balance. */
    private void earnedNet(String amount) {
        when(commissionService.earningsFor(tenantId, vendorId))
                .thenReturn(new VendorEarningsResponse(vendorId, new BigDecimal(amount), BigDecimal.ZERO));
    }

    private void payoutSums(String pending, String paid) {
        when(payoutRepository.sumAmount(tenantId, vendorId, PayoutStatus.PENDING)).thenReturn(new BigDecimal(pending));
        when(payoutRepository.sumAmount(tenantId, vendorId, PayoutStatus.PAID)).thenReturn(new BigDecimal(paid));
    }

    private PayoutRequest pending(BigDecimal amount) {
        PayoutRequest p = new PayoutRequest(tenantId, vendorId, amount);
        ReflectionTestUtils.setField(p, "id", payoutId);
        return p;
    }

    // ---- balance ------------------------------------------------------------

    @Test
    void balanceFor_subtractsPendingAndPaidFromEarned() {
        // Arrange — earned 100, 30 pending, 20 paid → 50 available
        earnedNet("100.00");
        payoutSums("30.00", "20.00");

        // Act
        PayoutBalanceResponse balance = service.balanceFor(tenantId, vendorId);

        // Assert
        assertThat(balance.earned()).isEqualByComparingTo("100.00");
        assertThat(balance.pending()).isEqualByComparingTo("30.00");
        assertThat(balance.paid()).isEqualByComparingTo("20.00");
        assertThat(balance.available()).isEqualByComparingTo("50.00");
    }

    // ---- request ------------------------------------------------------------

    @Test
    void request_withinBalance_createsPendingPayout_andAudits() {
        // Arrange — 50 available, request 40
        earnedNet("100.00");
        payoutSums("30.00", "20.00");
        when(payoutRepository.save(any(PayoutRequest.class))).thenAnswer(inv -> {
            PayoutRequest p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", payoutId);
            return p;
        });

        // Act
        PayoutResponse response = service.request(tenantId, vendorActor, new BigDecimal("40.00"));

        // Assert
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.amount()).isEqualByComparingTo("40.00");
        verify(auditService).recordSuccess(eq(AuditAction.PAYOUT_REQUESTED), eq(vendorActor),
                eq("PayoutRequest"), eq(payoutId.toString()), any());
    }

    @Test
    void request_exceedingBalance_isRejected_andAuditsFailure() {
        // Arrange — only 50 available, request 60
        earnedNet("100.00");
        payoutSums("30.00", "20.00");

        // Act & Assert
        assertThatThrownBy(() -> service.request(tenantId, vendorActor, new BigDecimal("60.00")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds your available balance");
        verify(payoutRepository, never()).save(any());
        verify(auditService).recordFailure(eq(AuditAction.PAYOUT_REQUESTED), eq(vendorActor),
                eq("PayoutRequest"), isNull(), any());
    }

    @Test
    void request_isRejected_whenOneIsAlreadyPending() {
        // Arrange — vendor already has a pending request
        when(payoutRepository.existsByTenantIdAndVendorIdAndStatus(tenantId, vendorId, PayoutStatus.PENDING))
                .thenReturn(true);

        // Act & Assert — guarded before any balance math or save
        assertThatThrownBy(() -> service.request(tenantId, vendorActor, new BigDecimal("10.00")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("pending payout");
        verify(payoutRepository, never()).save(any());
        verify(commissionService, never()).earningsFor(any(), any());
    }

    @Test
    void request_exactlyAtBalance_isAllowed() {
        // Arrange — 50 available, request exactly 50
        earnedNet("100.00");
        payoutSums("30.00", "20.00");
        when(payoutRepository.save(any(PayoutRequest.class))).thenAnswer(inv -> {
            PayoutRequest p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", payoutId);
            return p;
        });

        // Act
        PayoutResponse response = service.request(tenantId, vendorActor, new BigDecimal("50.00"));

        // Assert
        assertThat(response.amount()).isEqualByComparingTo("50.00");
    }

    // ---- pay / reject -------------------------------------------------------

    @Test
    void pay_marksPaid_recordsDeciderAndReference_andAudits() {
        // Arrange
        when(payoutRepository.findByIdAndTenantId(payoutId, tenantId))
                .thenReturn(Optional.of(pending(new BigDecimal("40.00"))));
        when(payoutRepository.save(any(PayoutRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PayoutResponse response = service.pay(payoutId, tenantId, adminActor, "CASH-001", "handed over");

        // Assert
        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.reference()).isEqualTo("CASH-001");
        assertThat(response.decidedBy()).isEqualTo(adminId);
        assertThat(response.decidedAt()).isNotNull();
        verify(auditService).recordSuccess(eq(AuditAction.PAYOUT_PAID), eq(adminActor),
                eq("PayoutRequest"), eq(payoutId.toString()), any());
    }

    @Test
    void pay_anAlreadyDecidedPayout_isRejected() {
        // Arrange — already paid
        PayoutRequest paid = pending(new BigDecimal("40.00"));
        paid.setStatus(PayoutStatus.PAID);
        when(payoutRepository.findByIdAndTenantId(payoutId, tenantId)).thenReturn(Optional.of(paid));

        // Act & Assert
        assertThatThrownBy(() -> service.pay(payoutId, tenantId, adminActor, null, null))
                .isInstanceOf(ConflictException.class);
        verify(payoutRepository, never()).save(any());
    }

    @Test
    void reject_marksRejectedWithReason_andAudits() {
        // Arrange
        when(payoutRepository.findByIdAndTenantId(payoutId, tenantId))
                .thenReturn(Optional.of(pending(new BigDecimal("40.00"))));
        when(payoutRepository.save(any(PayoutRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PayoutResponse response = service.reject(payoutId, tenantId, adminActor, "Insufficient documentation");

        // Assert
        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.resolutionNote()).isEqualTo("Insufficient documentation");
        verify(auditService).recordSuccess(eq(AuditAction.PAYOUT_REJECTED), eq(adminActor),
                eq("PayoutRequest"), eq(payoutId.toString()), any());
    }

    @Test
    void pay_missingPayout_is404() {
        // Arrange
        when(payoutRepository.findByIdAndTenantId(payoutId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.pay(payoutId, tenantId, adminActor, null, null))
                .isInstanceOf(NotFoundException.class);
    }
}
