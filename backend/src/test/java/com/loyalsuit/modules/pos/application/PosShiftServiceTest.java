package com.loyalsuit.modules.pos.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.modules.pos.application.dto.PosShiftResponse;
import com.loyalsuit.modules.pos.domain.PosShift;
import com.loyalsuit.modules.pos.domain.PosShiftStatus;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import com.loyalsuit.modules.pos.domain.port.PosShiftRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosShiftServiceTest {

    @Mock private PosShiftRepository shiftRepository;
    @Mock private PosSaleRepository saleRepository;

    @InjectMocks private PosShiftService service;

    private UUID tenantId;
    private UUID cashierId;
    private UUID shiftId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        cashierId = UUID.randomUUID();
        shiftId = UUID.randomUUID();
    }

    private PosShift openShift(String openingFloat) {
        PosShift shift = new PosShift(tenantId, cashierId, new BigDecimal(openingFloat));
        ReflectionTestUtils.setField(shift, "id", shiftId);
        return shift;
    }

    // ---- open ---------------------------------------------------------------

    @Test
    void open_createsAnOpenShift_withTheStartingFloat() {
        // Arrange
        when(shiftRepository.findOpenByCashier(tenantId, cashierId)).thenReturn(Optional.empty());
        when(shiftRepository.save(any(PosShift.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PosShiftResponse shift = service.open(tenantId, cashierId, new BigDecimal("100.00"));

        // Assert
        assertThat(shift.status()).isEqualTo("OPEN");
        assertThat(shift.openingFloat()).isEqualByComparingTo("100.00");
    }

    @Test
    void open_isRejected_whenOneIsAlreadyOpen() {
        // Arrange — the cashier already has an open drawer
        when(shiftRepository.findOpenByCashier(tenantId, cashierId)).thenReturn(Optional.of(openShift("50.00")));

        // Act & Assert
        assertThatThrownBy(() -> service.open(tenantId, cashierId, new BigDecimal("100.00")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have an open shift");
        verify(shiftRepository, never()).save(any());
    }

    // ---- close (reconciliation) ---------------------------------------------

    @Test
    void close_reconcilesTheDrawer_expectedIsFloatPlusCashSales() {
        // Arrange — float 100, all-cash sales 250 → expected 350; counted 350 → variance 0
        when(shiftRepository.findByIdAndTenantId(shiftId, tenantId)).thenReturn(Optional.of(openShift("100.00")));
        when(saleRepository.sumTotalByShift(tenantId, shiftId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.sumCashCollectedByShift(tenantId, shiftId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.countByShift(tenantId, shiftId)).thenReturn(7L);
        when(shiftRepository.save(any(PosShift.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PosShiftResponse shift = service.close(shiftId, tenantId, new BigDecimal("350.00"));

        // Assert
        assertThat(shift.status()).isEqualTo("CLOSED");
        assertThat(shift.salesTotal()).isEqualByComparingTo("250.00");
        assertThat(shift.cashSales()).isEqualByComparingTo("250.00");
        assertThat(shift.saleCount()).isEqualTo(7);
        assertThat(shift.expectedCash()).isEqualByComparingTo("350.00");
        assertThat(shift.countedCash()).isEqualByComparingTo("350.00");
        assertThat(shift.variance()).isEqualByComparingTo("0.00");
        assertThat(shift.closedAt()).isNotNull();
    }

    @Test
    void close_reconcilesAgainstCashOnly_excludingCardSales() {
        // Arrange — float 50; gross sales 300 but only 200 of it cash (100 on card)
        // → expected = 50 + 200 = 250; counted 250 → balanced. The card never hit the drawer.
        when(shiftRepository.findByIdAndTenantId(shiftId, tenantId)).thenReturn(Optional.of(openShift("50.00")));
        when(saleRepository.sumTotalByShift(tenantId, shiftId)).thenReturn(new BigDecimal("300.00"));
        when(saleRepository.sumCashCollectedByShift(tenantId, shiftId)).thenReturn(new BigDecimal("200.00"));
        when(saleRepository.countByShift(tenantId, shiftId)).thenReturn(5L);
        when(shiftRepository.save(any(PosShift.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PosShiftResponse shift = service.close(shiftId, tenantId, new BigDecimal("250.00"));

        // Assert
        assertThat(shift.salesTotal()).isEqualByComparingTo("300.00");
        assertThat(shift.cashSales()).isEqualByComparingTo("200.00");
        assertThat(shift.expectedCash()).isEqualByComparingTo("250.00");
        assertThat(shift.variance()).isEqualByComparingTo("0.00");
    }

    @Test
    void close_reportsAShortDrawerAsNegativeVariance() {
        // Arrange — expected 350, but only 340 counted → 10 short
        when(shiftRepository.findByIdAndTenantId(shiftId, tenantId)).thenReturn(Optional.of(openShift("100.00")));
        when(saleRepository.sumTotalByShift(tenantId, shiftId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.sumCashCollectedByShift(tenantId, shiftId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.countByShift(tenantId, shiftId)).thenReturn(7L);
        when(shiftRepository.save(any(PosShift.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PosShiftResponse shift = service.close(shiftId, tenantId, new BigDecimal("340.00"));

        // Assert
        assertThat(shift.variance()).isEqualByComparingTo("-10.00");
    }

    @Test
    void close_reportsAnOverDrawerAsPositiveVariance() {
        // Arrange — expected 350, but 360 counted → 10 over
        when(shiftRepository.findByIdAndTenantId(shiftId, tenantId)).thenReturn(Optional.of(openShift("100.00")));
        when(saleRepository.sumTotalByShift(tenantId, shiftId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.sumCashCollectedByShift(tenantId, shiftId)).thenReturn(new BigDecimal("250.00"));
        when(saleRepository.countByShift(tenantId, shiftId)).thenReturn(7L);
        when(shiftRepository.save(any(PosShift.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        PosShiftResponse shift = service.close(shiftId, tenantId, new BigDecimal("360.00"));

        // Assert
        assertThat(shift.variance()).isEqualByComparingTo("10.00");
    }

    @Test
    void close_anAlreadyClosedShift_isRejected() {
        // Arrange
        PosShift closed = openShift("100.00");
        closed.setStatus(PosShiftStatus.CLOSED);
        when(shiftRepository.findByIdAndTenantId(shiftId, tenantId)).thenReturn(Optional.of(closed));

        // Act & Assert
        assertThatThrownBy(() -> service.close(shiftId, tenantId, new BigDecimal("100.00")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already closed");
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void close_aMissingShift_is404() {
        // Arrange
        when(shiftRepository.findByIdAndTenantId(shiftId, tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.close(shiftId, tenantId, new BigDecimal("100.00")))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- current ------------------------------------------------------------

    @Test
    void current_isNull_whenNoShiftIsOpen() {
        // Arrange
        when(shiftRepository.findOpenByCashier(tenantId, cashierId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(service.current(tenantId, cashierId)).isNull();
    }
}
