package com.loyalsuit.modules.marketplace.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A vendor order line handed to the commission engine for settlement. Kept free of
 * any orders-module type so the marketplace stays decoupled from orders — the caller
 * (order management) maps its order items into these.
 *
 * @param vendorId    the vendor's user id (the line's selling vendor)
 * @param orderItemId the order line this commission is earned on (settlement key)
 * @param grossAmount the line total the commission is computed from
 */
public record CommissionLine(UUID vendorId, UUID orderItemId, BigDecimal grossAmount) {
}
