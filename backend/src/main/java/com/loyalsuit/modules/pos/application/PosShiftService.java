package com.loyalsuit.modules.pos.application;

import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.pos.application.dto.PosShiftResponse;
import com.loyalsuit.modules.pos.domain.PosShift;
import com.loyalsuit.modules.pos.domain.port.PosSaleRepository;
import com.loyalsuit.modules.pos.domain.port.PosShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cash-drawer sessions. A cashier opens a shift with a starting float, rings up sales
 * against it, then closes it by counting the drawer — at which point the till is
 * reconciled against the cash portion of those sales (expected = float + cashSales;
 * variance = counted − expected; card payments never touch the drawer). At most one
 * shift is open per cashier at a time.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PosShiftService {

    private final PosShiftRepository shiftRepository;
    private final PosSaleRepository saleRepository;

    /** The cashier's currently-open drawer, or null if they have none. */
    public PosShiftResponse current(UUID tenantId, UUID cashierId) {
        return shiftRepository.findOpenByCashier(tenantId, cashierId)
                .map(PosShiftResponse::from)
                .orElse(null);
    }

    @Transactional
    public PosShiftResponse open(UUID tenantId, UUID cashierId, BigDecimal openingFloat) {
        // One open drawer per cashier; the partial unique index is the concurrency backstop.
        if (shiftRepository.findOpenByCashier(tenantId, cashierId).isPresent()) {
            throw new ConflictException("You already have an open shift");
        }
        PosShift shift = shiftRepository.save(new PosShift(tenantId, cashierId, openingFloat));
        return PosShiftResponse.from(shift);
    }

    @Transactional
    public PosShiftResponse close(UUID id, UUID tenantId, BigDecimal countedCash) {
        PosShift shift = shiftRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Shift", id));
        if (!shift.isOpen()) {
            throw new ConflictException("This shift is already closed");
        }
        BigDecimal salesTotal = saleRepository.sumTotalByShift(tenantId, shift.getId());
        BigDecimal cashSales = saleRepository.sumCashCollectedByShift(tenantId, shift.getId());
        long saleCount = saleRepository.countByShift(tenantId, shift.getId());
        shift.close(countedCash, salesTotal, cashSales, (int) saleCount);
        return PosShiftResponse.from(shiftRepository.save(shift));
    }

    public PageResponse<PosShiftResponse> list(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(shiftRepository.findByTenantId(tenantId, pageable).map(PosShiftResponse::from));
    }
}
