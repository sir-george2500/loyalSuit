package com.loyalsuit.modules.orders.domain;

/**
 * Order fulfilment lifecycle with explicit, guarded transitions. An order can only
 * move along a valid edge; anything else is rejected by the service. Cancellation
 * releases reserved stock; REFUNDED is reached from DELIVERED (returns flow).
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == PROCESSING || target == CANCELLED;
            case PROCESSING -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED -> target == REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
    }

    /** Stock was reserved at checkout; these states still hold it (so cancel releases it). */
    public boolean holdsReservedStock() {
        return this == PENDING || this == CONFIRMED || this == PROCESSING;
    }
}
