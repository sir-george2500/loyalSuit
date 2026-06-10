package com.loyalsuit.modules.orders.application.port;

import java.math.BigDecimal;

/** A priced delivery zone: the fee to charge, plus labels snapshotted onto the order. */
public record ResolvedShippingZone(String zoneName, String pickupPointName, BigDecimal fee) {
}
