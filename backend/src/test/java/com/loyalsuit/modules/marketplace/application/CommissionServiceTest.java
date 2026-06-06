package com.loyalsuit.modules.marketplace.application;

import com.loyalsuit.modules.marketplace.application.dto.VendorEarningsResponse;
import com.loyalsuit.modules.marketplace.domain.CommissionEntry;
import com.loyalsuit.modules.marketplace.domain.CommissionStatus;
import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.port.CommissionEntryRepository;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock private CommissionEntryRepository commissionRepository;
    @Mock private VendorRepository vendorRepository;

    @InjectMocks private CommissionService service;

    private UUID tenantId;
    private UUID orderId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
    }

    private Vendor vendorWithRate(String rate) {
        Vendor v = new Vendor(tenantId, vendorId, "Store", "store");
        v.setCommissionRate(new BigDecimal(rate));
        return v;
    }

    private CommissionLine line(BigDecimal gross) {
        return new CommissionLine(vendorId, UUID.randomUUID(), gross);
    }

    // ---- settlement ---------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void settleOrder_earnsCommissionAtTheVendorRate() {
        // Arrange — 15% of a $40 line: $6 commission, $34 net
        when(commissionRepository.existsByOrderId(orderId)).thenReturn(false);
        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.of(vendorWithRate("15.00")));

        // Act
        service.settleOrder(tenantId, orderId, "ORD-1", List.of(line(BigDecimal.valueOf(40))));

        // Assert
        ArgumentCaptor<List<CommissionEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(e -> {
            assertThat(e.getVendorId()).isEqualTo(vendorId);
            assertThat(e.getOrderNumber()).isEqualTo("ORD-1");
            assertThat(e.getCommissionRate()).isEqualByComparingTo("15.00");
            assertThat(e.getCommissionAmount()).isEqualByComparingTo("6.00");
            assertThat(e.getNetAmount()).isEqualByComparingTo("34.00");
            assertThat(e.getStatus()).isEqualTo(CommissionStatus.EARNED);
        });
    }

    @Test
    void settleOrder_isIdempotent_whenOrderAlreadySettled() {
        // Arrange — the order already has entries
        when(commissionRepository.existsByOrderId(orderId)).thenReturn(true);

        // Act
        service.settleOrder(tenantId, orderId, "ORD-1", List.of(line(BigDecimal.TEN)));

        // Assert — nothing re-earned
        verify(commissionRepository, never()).saveAll(any());
        verify(vendorRepository, never()).findByUserId(any());
    }

    @Test
    void settleOrder_withNoLines_doesNothing() {
        // Act
        service.settleOrder(tenantId, orderId, "ORD-1", List.of());

        // Assert
        verify(commissionRepository, never()).existsByOrderId(any());
        verify(commissionRepository, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void settleOrder_skipsLinesWithNoVendorRecord() {
        // Arrange — vendor lookup fails (shouldn't happen in practice): skip, don't mis-charge
        when(commissionRepository.existsByOrderId(orderId)).thenReturn(false);
        when(vendorRepository.findByUserId(vendorId)).thenReturn(Optional.empty());

        // Act
        service.settleOrder(tenantId, orderId, "ORD-1", List.of(line(BigDecimal.TEN)));

        // Assert — an empty batch is saved (no entry for the unknown vendor)
        ArgumentCaptor<List<CommissionEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    // ---- reversal -----------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void reverseOrder_reversesEarnedEntriesOnly() {
        // Arrange — one earned, one already reversed
        CommissionEntry earned = new CommissionEntry(tenantId, vendorId, orderId, UUID.randomUUID(),
                "ORD-1", BigDecimal.valueOf(40), new BigDecimal("10.00"));
        CommissionEntry alreadyReversed = new CommissionEntry(tenantId, vendorId, orderId, UUID.randomUUID(),
                "ORD-1", BigDecimal.valueOf(20), new BigDecimal("10.00"));
        alreadyReversed.reverse();
        when(commissionRepository.findByOrderId(orderId)).thenReturn(List.of(earned, alreadyReversed));

        // Act
        service.reverseOrder(tenantId, orderId);

        // Assert — only the earned entry is flipped and saved
        ArgumentCaptor<List<CommissionEntry>> captor = ArgumentCaptor.forClass(List.class);
        verify(commissionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .satisfies(e -> assertThat(e.getStatus()).isEqualTo(CommissionStatus.REVERSED));
        assertThat(earned.getStatus()).isEqualTo(CommissionStatus.REVERSED);
    }

    @Test
    void reverseOrder_unsettledOrder_isANoOpSave() {
        // Arrange — no entries for the order
        when(commissionRepository.findByOrderId(orderId)).thenReturn(List.of());

        // Act
        service.reverseOrder(tenantId, orderId);

        // Assert — an empty save (nothing to reverse)
        verify(commissionRepository).saveAll(anyList());
    }

    // ---- earnings -----------------------------------------------------------

    @Test
    void earningsFor_reportsEarnedAndReversedNet() {
        // Arrange
        when(commissionRepository.sumNetAmount(tenantId, vendorId, CommissionStatus.EARNED))
                .thenReturn(new BigDecimal("120.00"));
        when(commissionRepository.sumNetAmount(tenantId, vendorId, CommissionStatus.REVERSED))
                .thenReturn(new BigDecimal("34.00"));

        // Act
        VendorEarningsResponse earnings = service.earningsFor(tenantId, vendorId);

        // Assert
        assertThat(earnings.vendorId()).isEqualTo(vendorId);
        assertThat(earnings.earnedBalance()).isEqualByComparingTo("120.00");
        assertThat(earnings.reversedTotal()).isEqualByComparingTo("34.00");
    }
}
