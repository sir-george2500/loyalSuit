package com.loyalsuit.modules.orders.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Per-customer order rollup (count + lifetime spend) for the admin customer list. */
public record CustomerOrderStat(UUID customerId, long orderCount, BigDecimal totalSpent) {}
