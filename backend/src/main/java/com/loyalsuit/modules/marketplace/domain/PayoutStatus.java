package com.loyalsuit.modules.marketplace.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a payout request. A vendor's request starts {@code PENDING}; an admin
 * either {@code PAID}s it (cash handed over) or {@code REJECT}s it. Both decisions are
 * terminal — the guard below makes a paid request impossible to re-decide.
 */
public enum PayoutStatus {
    PENDING,
    PAID,
    REJECTED;

    private static final Set<PayoutStatus> FROM_PENDING = EnumSet.of(PAID, REJECTED);

    public boolean canTransitionTo(PayoutStatus target) {
        return this == PENDING && FROM_PENDING.contains(target);
    }
}
