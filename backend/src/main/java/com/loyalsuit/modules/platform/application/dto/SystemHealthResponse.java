package com.loyalsuit.modules.platform.application.dto;

import java.util.Map;

/**
 * A snapshot of platform health: database connectivity, key entity counts, and JVM stats.
 */
public record SystemHealthResponse(
        boolean databaseUp,
        Map<String, Long> counts,
        JvmInfo jvm) {

    public record JvmInfo(
            long uptimeMs,
            long memoryUsedMb,
            long memoryMaxMb,
            int availableProcessors,
            String javaVersion) {}
}
