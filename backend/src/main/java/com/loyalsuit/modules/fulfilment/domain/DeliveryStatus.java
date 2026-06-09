package com.loyalsuit.modules.fulfilment.domain;

/**
 * A delivery's lifecycle with explicit, guarded transitions. A delivery is born PENDING
 * (created, awaiting a courier), gets ASSIGNED, then the agent moves it PICKED_UP →
 * IN_TRANSIT → DELIVERED. Any active state can end in FAILED. DELIVERED and FAILED are
 * terminal. Assignment itself (PENDING → ASSIGNED, and reassigning before pickup) is
 * handled on the aggregate, not through this edge table.
 */
public enum DeliveryStatus {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED;

    public boolean canTransitionTo(DeliveryStatus target) {
        return switch (this) {
            case ASSIGNED -> target == PICKED_UP || target == FAILED;
            case PICKED_UP -> target == IN_TRANSIT || target == FAILED;
            case IN_TRANSIT -> target == DELIVERED || target == FAILED;
            case PENDING, DELIVERED, FAILED -> false;
        };
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == FAILED;
    }
}
