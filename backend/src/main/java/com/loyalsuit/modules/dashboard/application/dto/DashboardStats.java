package com.loyalsuit.modules.dashboard.application.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated, tenant-scoped analytics for the admin dashboard. Headline figures
 * carry period-over-period comparison; trend/breakdown/recent sections give the
 * operator context rather than bare counts.
 */
public record DashboardStats(
        KpiMetric revenue,
        KpiMetric orders,
        KpiMetric customers,
        BigDecimal averageOrderValue,
        long totalProducts,
        long activeProducts,
        long lowStockCount,
        long totalVendors,
        List<RevenuePoint> revenueTrend,
        List<StatusCount> ordersByStatus,
        List<RecentOrder> recentOrders
) {
}
