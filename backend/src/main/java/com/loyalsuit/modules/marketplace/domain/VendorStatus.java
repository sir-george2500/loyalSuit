package com.loyalsuit.modules.marketplace.domain;

/**
 * Vendor lifecycle. A new application is PENDING; an admin approves (ACTIVE),
 * rejects (terminal), or later suspends/reinstates an active vendor.
 */
public enum VendorStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    REJECTED;

    public boolean canTransitionTo(VendorStatus target) {
        return switch (this) {
            case PENDING -> target == ACTIVE || target == REJECTED;
            case ACTIVE -> target == SUSPENDED;
            case SUSPENDED -> target == ACTIVE;
            case REJECTED -> false;
        };
    }
}
