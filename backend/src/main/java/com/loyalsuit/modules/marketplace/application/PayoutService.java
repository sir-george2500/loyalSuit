package com.loyalsuit.modules.marketplace.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.ConflictException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.audit.application.AuditActor;
import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.audit.domain.AuditAction;
import com.loyalsuit.modules.marketplace.application.dto.PayoutBalanceResponse;
import com.loyalsuit.modules.marketplace.application.dto.PayoutResponse;
import com.loyalsuit.modules.marketplace.domain.PayoutRequest;
import com.loyalsuit.modules.marketplace.domain.PayoutStatus;
import com.loyalsuit.modules.marketplace.domain.port.PayoutRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Vendor payouts drawn against settled commission. A vendor requests an amount up to
 * their available balance (earned net, less pending + paid payouts); an admin pays it
 * (cash) or rejects it. Every action is written to the audit trail, and the pay/reject
 * decision is guarded by the request's optimistic version so it can't be double-applied.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayoutService {

    private final PayoutRequestRepository payoutRepository;
    private final CommissionService commissionService;
    private final AuditService auditService;

    /** A vendor's payout standing: earned net, locked (pending), paid, and what's left. */
    public PayoutBalanceResponse balanceFor(UUID tenantId, UUID vendorUserId) {
        BigDecimal earned = commissionService.earningsFor(tenantId, vendorUserId).earnedBalance();
        BigDecimal pending = payoutRepository.sumAmount(tenantId, vendorUserId, PayoutStatus.PENDING);
        BigDecimal paid = payoutRepository.sumAmount(tenantId, vendorUserId, PayoutStatus.PAID);
        BigDecimal available = earned.subtract(pending).subtract(paid);
        return new PayoutBalanceResponse(vendorUserId, earned, pending, paid, available);
    }

    @Transactional
    public PayoutResponse request(UUID tenantId, AuditActor actor, BigDecimal amount) {
        // One pending request at a time keeps the available-balance math unambiguous;
        // the partial unique index is the hard backstop against a concurrent duplicate.
        if (payoutRepository.existsByTenantIdAndVendorIdAndStatus(tenantId, actor.userId(), PayoutStatus.PENDING)) {
            throw new ConflictException("You already have a pending payout request");
        }
        BigDecimal available = balanceFor(tenantId, actor.userId()).available();
        if (amount.compareTo(available) > 0) {
            auditService.recordFailure(AuditAction.PAYOUT_REQUESTED, actor, "PayoutRequest", null,
                    "amount=" + amount + " exceeds available=" + available);
            throw new BusinessException("Payout exceeds your available balance of " + available);
        }
        PayoutRequest saved = payoutRepository.save(new PayoutRequest(tenantId, actor.userId(), amount));
        auditService.recordSuccess(AuditAction.PAYOUT_REQUESTED, actor, "PayoutRequest",
                saved.getId().toString(), "amount=" + amount);
        return PayoutResponse.from(saved);
    }

    public PageResponse<PayoutResponse> listMine(UUID tenantId, UUID vendorUserId, Pageable pageable) {
        return new PageResponse<>(payoutRepository
                .findByTenantIdAndVendorId(tenantId, vendorUserId, pageable)
                .map(PayoutResponse::from));
    }

    public PageResponse<PayoutResponse> list(UUID tenantId, PayoutStatus status, Pageable pageable) {
        var page = status == null
                ? payoutRepository.findByTenantId(tenantId, pageable)
                : payoutRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return new PageResponse<>(page.map(PayoutResponse::from));
    }

    @Transactional
    public PayoutResponse pay(UUID id, UUID tenantId, AuditActor admin, String reference, String note) {
        PayoutRequest payout = decide(id, tenantId, PayoutStatus.PAID, admin);
        payout.setReference(trimToNull(reference));
        payout.setResolutionNote(trimToNull(note));
        PayoutRequest saved = payoutRepository.save(payout);
        auditService.recordSuccess(AuditAction.PAYOUT_PAID, admin, "PayoutRequest", id.toString(),
                "amount=" + saved.getAmount() + (saved.getReference() != null ? " ref=" + saved.getReference() : ""));
        return PayoutResponse.from(saved);
    }

    @Transactional
    public PayoutResponse reject(UUID id, UUID tenantId, AuditActor admin, String note) {
        PayoutRequest payout = decide(id, tenantId, PayoutStatus.REJECTED, admin);
        payout.setResolutionNote(note.trim());
        PayoutRequest saved = payoutRepository.save(payout);
        auditService.recordSuccess(AuditAction.PAYOUT_REJECTED, admin, "PayoutRequest", id.toString(),
                "reason=" + note.trim());
        return PayoutResponse.from(saved);
    }

    /** Loads a pending payout and applies the admin's terminal decision (guarded). */
    private PayoutRequest decide(UUID id, UUID tenantId, PayoutStatus target, AuditActor admin) {
        PayoutRequest payout = payoutRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Payout", id));
        if (!payout.getStatus().canTransitionTo(target)) {
            throw new ConflictException("This payout has already been " + payout.getStatus());
        }
        payout.setStatus(target);
        payout.setDecidedBy(admin.userId());
        payout.setDecidedAt(Instant.now());
        return payout;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
