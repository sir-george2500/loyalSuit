package com.loyalsuit.modules.orders.domain;

/**
 * How an order is paid. Storefront checkout is cash-on-delivery; the POS also takes CARD
 * (charged on an external terminal — not an online gateway) and SPLIT (cash + card on one
 * sale). Online gateway methods arrive in Phase 4.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    SPLIT
}
