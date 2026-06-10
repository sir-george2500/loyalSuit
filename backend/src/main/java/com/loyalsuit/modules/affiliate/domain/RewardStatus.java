package com.loyalsuit.modules.affiliate.domain;

/**
 * Lifecycle of an affiliate reward entry. EARNED when the referred order is paid; REVERSED
 * if that order is later refunded (kept for audit, no longer counts toward the balance).
 */
public enum RewardStatus {
    EARNED,
    REVERSED
}
