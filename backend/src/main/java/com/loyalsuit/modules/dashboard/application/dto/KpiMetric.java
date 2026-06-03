package com.loyalsuit.modules.dashboard.application.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A headline metric with period-over-period comparison.
 *
 * @param current        value for the current period
 * @param previous       value for the immediately preceding period of equal length
 * @param changePercent  signed percentage change vs the previous period (1 dp)
 */
public record KpiMetric(BigDecimal current, BigDecimal previous, double changePercent) {

    public static KpiMetric of(BigDecimal current, BigDecimal previous) {
        BigDecimal cur = current != null ? current : BigDecimal.ZERO;
        BigDecimal prev = previous != null ? previous : BigDecimal.ZERO;

        double change;
        if (prev.signum() == 0) {
            change = cur.signum() > 0 ? 100.0 : 0.0;
        } else {
            change = cur.subtract(prev)
                    .divide(prev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
        return new KpiMetric(cur, prev, change);
    }

    public static KpiMetric of(long current, long previous) {
        return of(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
}
