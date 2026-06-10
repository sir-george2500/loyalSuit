package com.loyalsuit.modules.loyalty.domain;

/** A movement on a loyalty account. */
public enum LoyaltyTransactionType {
    /** Points credited when a customer's order is paid. */
    EARN,
    /** Points spent against an order's discount. */
    REDEEM,
    /** Points clawed back when a paid order is refunded. */
    REVERSE
}
