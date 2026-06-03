package com.loyalsuit.modules.dashboard.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single day on the revenue trend line. */
public record RevenuePoint(LocalDate date, BigDecimal amount) {
}
