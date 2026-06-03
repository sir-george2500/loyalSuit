package com.loyalsuit.modules.dashboard.infrastructure.persistence;

import com.loyalsuit.modules.dashboard.application.dto.DashboardStats;
import com.loyalsuit.modules.dashboard.application.dto.KpiMetric;
import com.loyalsuit.modules.dashboard.application.dto.RecentOrder;
import com.loyalsuit.modules.dashboard.application.dto.RevenuePoint;
import com.loyalsuit.modules.dashboard.application.dto.StatusCount;
import com.loyalsuit.modules.dashboard.domain.port.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Computes tenant-scoped dashboard analytics: period-over-period KPIs, a daily
 * revenue trend, order-status breakdown and recent orders. Each section is a
 * focused query; all are tenant-filtered.
 */
@Repository
@RequiredArgsConstructor
public class DashboardRepositoryAdapter implements DashboardRepository {

    private static final int PERIOD_DAYS = 30;
    private static final int TREND_DAYS = 14;
    private static final int RECENT_LIMIT = 5;
    private static final String EXCLUDED_STATUSES = "('CANCELLED','REFUNDED')";

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public DashboardStats statsForTenant(UUID tenantId) {
        Instant now = Instant.now();
        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("currentStart", Timestamp.from(now.minus(Duration.ofDays(PERIOD_DAYS))))
                .addValue("previousStart", Timestamp.from(now.minus(Duration.ofDays(2L * PERIOD_DAYS))));

        KpiBundle kpis = loadKpis(params);
        CustomerBundle customers = loadCustomers(params);
        CatalogBundle catalog = loadCatalog(tenantId);

        BigDecimal avgOrderValue = kpis.ordersCurrent > 0
                ? kpis.revenueCurrent.divide(BigDecimal.valueOf(kpis.ordersCurrent), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new DashboardStats(
                KpiMetric.of(kpis.revenueCurrent, kpis.revenuePrevious),
                KpiMetric.of(kpis.ordersCurrent, kpis.ordersPrevious),
                KpiMetric.of(customers.current, customers.previous),
                avgOrderValue,
                catalog.totalProducts,
                catalog.activeProducts,
                catalog.lowStock,
                catalog.totalVendors,
                loadRevenueTrend(tenantId, now),
                loadOrdersByStatus(tenantId),
                loadRecentOrders(tenantId)
        );
    }

    private KpiBundle loadKpis(MapSqlParameterSource params) {
        String sql = """
                SELECT
                  COALESCE(SUM(total) FILTER (
                    WHERE created_at >= :currentStart AND status NOT IN %1$s), 0) AS rev_cur,
                  COALESCE(SUM(total) FILTER (
                    WHERE created_at >= :previousStart AND created_at < :currentStart
                      AND status NOT IN %1$s), 0) AS rev_prev,
                  COUNT(*) FILTER (WHERE created_at >= :currentStart) AS ord_cur,
                  COUNT(*) FILTER (
                    WHERE created_at >= :previousStart AND created_at < :currentStart) AS ord_prev
                FROM orders
                WHERE tenant_id = :tenantId
                """.formatted(EXCLUDED_STATUSES);

        return jdbc.queryForObject(sql, params, (rs, n) -> new KpiBundle(
                rs.getBigDecimal("rev_cur"),
                rs.getBigDecimal("rev_prev"),
                rs.getLong("ord_cur"),
                rs.getLong("ord_prev")
        ));
    }

    private CustomerBundle loadCustomers(MapSqlParameterSource params) {
        String sql = """
                SELECT
                  COUNT(*) FILTER (
                    WHERE created_at >= :currentStart AND role = 'CUSTOMER') AS cust_cur,
                  COUNT(*) FILTER (
                    WHERE created_at >= :previousStart AND created_at < :currentStart
                      AND role = 'CUSTOMER') AS cust_prev
                FROM app_users
                WHERE tenant_id = :tenantId
                """;

        return jdbc.queryForObject(sql, params, (rs, n) ->
                new CustomerBundle(rs.getLong("cust_cur"), rs.getLong("cust_prev")));
    }

    private CatalogBundle loadCatalog(UUID tenantId) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM products WHERE tenant_id = :tenantId) AS total_products,
                  (SELECT COUNT(*) FROM products WHERE tenant_id = :tenantId AND status = 'ACTIVE') AS active_products,
                  (SELECT COUNT(*) FROM vendors WHERE tenant_id = :tenantId) AS total_vendors,
                  (SELECT COUNT(*) FROM stock WHERE tenant_id = :tenantId
                     AND quantity <= low_stock_threshold) AS low_stock
                """;
        var params = new MapSqlParameterSource("tenantId", tenantId);
        return jdbc.queryForObject(sql, params, (rs, n) -> new CatalogBundle(
                rs.getLong("total_products"),
                rs.getLong("active_products"),
                rs.getLong("total_vendors"),
                rs.getLong("low_stock")
        ));
    }

    private List<RevenuePoint> loadRevenueTrend(UUID tenantId, Instant now) {
        Instant from = now.minus(Duration.ofDays(TREND_DAYS - 1L));
        String sql = """
                SELECT CAST(created_at AS DATE) AS d, COALESCE(SUM(total), 0) AS amt
                FROM orders
                WHERE tenant_id = :tenantId
                  AND status NOT IN %s
                  AND created_at >= :from
                GROUP BY d
                """.formatted(EXCLUDED_STATUSES);

        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("from", Timestamp.from(from));

        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        jdbc.query(sql, params, (RowCallbackHandler) rs ->
                byDay.put(rs.getObject("d", LocalDate.class), rs.getBigDecimal("amt")));

        // Zero-fill so the trend line is continuous even when there are no sales on a day.
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<RevenuePoint> points = new ArrayList<>(TREND_DAYS);
        for (int i = TREND_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            points.add(new RevenuePoint(day, byDay.getOrDefault(day, BigDecimal.ZERO)));
        }
        return points;
    }

    private List<StatusCount> loadOrdersByStatus(UUID tenantId) {
        String sql = """
                SELECT status, COUNT(*) AS c
                FROM orders
                WHERE tenant_id = :tenantId
                GROUP BY status
                ORDER BY c DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("tenantId", tenantId),
                (rs, n) -> new StatusCount(rs.getString("status"), rs.getLong("c")));
    }

    private List<RecentOrder> loadRecentOrders(UUID tenantId) {
        String sql = """
                SELECT o.order_number, o.total, o.status, o.created_at,
                       COALESCE(u.full_name, u.email) AS customer_name
                FROM orders o
                LEFT JOIN app_users u ON u.id = o.customer_id
                WHERE o.tenant_id = :tenantId
                ORDER BY o.created_at DESC
                LIMIT :limit
                """;
        var params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("limit", RECENT_LIMIT);
        return jdbc.query(sql, params, (rs, n) -> new RecentOrder(
                rs.getString("order_number"),
                rs.getBigDecimal("total"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getString("customer_name")
        ));
    }

    private record KpiBundle(BigDecimal revenueCurrent, BigDecimal revenuePrevious,
                             long ordersCurrent, long ordersPrevious) {}

    private record CustomerBundle(long current, long previous) {}

    private record CatalogBundle(long totalProducts, long activeProducts,
                                 long totalVendors, long lowStock) {}
}
