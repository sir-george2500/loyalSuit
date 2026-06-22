package com.loyalsuit.modules.platform.application;

import com.loyalsuit.modules.platform.application.dto.SystemHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight platform health snapshot for the System Health screen. Counts are read with
 * a plain JdbcTemplate so this never couples to other modules' repositories; a failed count
 * marks the database as down rather than failing the request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService {

    private static final long MB = 1024L * 1024L;

    /** Tables surfaced as counts, in display order. */
    private static final List<String> COUNTED_TABLES =
            List.of("tenants", "app_users", "products", "orders", "vendors", "reviews");

    private final JdbcTemplate jdbcTemplate;

    public SystemHealthResponse snapshot() {
        boolean dbUp = true;
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : COUNTED_TABLES) {
            try {
                Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
                counts.put(table, n == null ? 0L : n);
            } catch (RuntimeException e) {
                log.warn("System health: count for table {} failed", table, e);
                dbUp = false;
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / MB;
        long max = runtime.maxMemory() / MB;
        SystemHealthResponse.JvmInfo jvm = new SystemHealthResponse.JvmInfo(
                ManagementFactory.getRuntimeMXBean().getUptime(),
                used, max, runtime.availableProcessors(), System.getProperty("java.version"));

        return new SystemHealthResponse(dbUp, counts, jvm);
    }
}
