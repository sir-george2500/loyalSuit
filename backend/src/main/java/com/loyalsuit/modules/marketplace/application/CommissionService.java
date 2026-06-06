package com.loyalsuit.modules.marketplace.application;

import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.marketplace.application.dto.CommissionEntryResponse;
import com.loyalsuit.modules.marketplace.application.dto.VendorEarningsResponse;
import com.loyalsuit.modules.marketplace.domain.CommissionEntry;
import com.loyalsuit.modules.marketplace.domain.CommissionStatus;
import com.loyalsuit.modules.marketplace.domain.Vendor;
import com.loyalsuit.modules.marketplace.domain.port.CommissionEntryRepository;
import com.loyalsuit.modules.marketplace.domain.port.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The commission engine and ledger. Commission is <b>earned</b> when an order's cash
 * is collected (the order is marked paid) and <b>reversed</b> if that order is later
 * refunded. Every entry snapshots the vendor's rate and the resulting amounts, so the
 * ledger is fully auditable and a vendor's owed balance is just the sum of EARNED net.
 *
 * <p>Settlement is idempotent (an order settles once); reversal is idempotent too.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommissionService {

    private final CommissionEntryRepository commissionRepository;
    private final VendorRepository vendorRepository;

    /**
     * Earns commission on every vendor line of a just-paid order. A no-op when the
     * order has no vendor lines or has already been settled (idempotent on order).
     */
    @Transactional
    public void settleOrder(UUID tenantId, UUID orderId, String orderNumber, List<CommissionLine> lines) {
        if (lines.isEmpty() || commissionRepository.existsByOrderId(orderId)) {
            return;
        }

        Map<UUID, BigDecimal> rateByVendor = new HashMap<>();
        List<CommissionEntry> entries = new ArrayList<>();
        for (CommissionLine line : lines) {
            BigDecimal rate = rateByVendor.computeIfAbsent(line.vendorId(), this::rateFor);
            if (rate == null) {
                continue; // No vendor record (shouldn't happen) — skip rather than mis-charge.
            }
            entries.add(new CommissionEntry(tenantId, line.vendorId(), orderId, line.orderItemId(),
                    orderNumber, line.grossAmount(), rate));
        }
        commissionRepository.saveAll(entries);
    }

    /**
     * Reverses every earned entry of a refunded order. Idempotent — already-reversed
     * entries stay reversed, and an unsettled order is a no-op.
     */
    @Transactional
    public void reverseOrder(UUID tenantId, UUID orderId) {
        List<CommissionEntry> earned = commissionRepository.findByOrderId(orderId).stream()
                .filter(entry -> entry.getStatus() == CommissionStatus.EARNED)
                .toList();
        earned.forEach(CommissionEntry::reverse);
        commissionRepository.saveAll(earned);
    }

    /** A vendor's own commission ledger (most recent first). */
    public PageResponse<CommissionEntryResponse> listForVendor(UUID tenantId, UUID vendorUserId, Pageable pageable) {
        return new PageResponse<>(commissionRepository
                .findByTenantIdAndVendorId(tenantId, vendorUserId, pageable)
                .map(CommissionEntryResponse::from));
    }

    /** The tenant-wide commission ledger (admin/owner view). */
    public PageResponse<CommissionEntryResponse> listForTenant(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(commissionRepository
                .findByTenantId(tenantId, pageable)
                .map(CommissionEntryResponse::from));
    }

    /** A vendor's earned (owed) balance and the total reversed by refunds. */
    public VendorEarningsResponse earningsFor(UUID tenantId, UUID vendorUserId) {
        return new VendorEarningsResponse(
                vendorUserId,
                commissionRepository.sumNetAmount(tenantId, vendorUserId, CommissionStatus.EARNED),
                commissionRepository.sumNetAmount(tenantId, vendorUserId, CommissionStatus.REVERSED));
    }

    /** Current commission rate for a vendor (by user id); null when no vendor exists. */
    private BigDecimal rateFor(UUID vendorUserId) {
        return vendorRepository.findByUserId(vendorUserId)
                .map(Vendor::getCommissionRate)
                .orElse(null);
    }
}
