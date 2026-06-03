package com.loyalsuit.modules.dashboard.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** A compact order row for the dashboard's recent-activity list. */
public record RecentOrder(
        String orderNumber,
        BigDecimal total,
        String status,
        Instant createdAt,
        String customerName
) {
}
