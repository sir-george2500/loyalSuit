package com.loyalsuit.modules.orders.application.dto;

import java.util.UUID;

/**
 * A line a customer actually received: the product and the vendor that sold it
 * ({@code vendorId} is null for a house product). Published so other modules can verify
 * a purchase without reaching into orders' internals — e.g. reviews gating on delivery.
 */
public record PurchasedItem(UUID productId, UUID vendorId) {}
