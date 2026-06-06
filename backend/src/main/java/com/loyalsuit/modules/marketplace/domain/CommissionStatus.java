package com.loyalsuit.modules.marketplace.domain;

/**
 * Lifecycle of a commission ledger entry.
 *
 * <ul>
 *   <li>{@code EARNED} — the order's cash was collected; the vendor is owed the net.</li>
 *   <li>{@code REVERSED} — the order was refunded; the entry no longer counts toward
 *       the vendor's balance, but is kept for audit (never deleted).</li>
 * </ul>
 */
public enum CommissionStatus {
    EARNED,
    REVERSED
}
