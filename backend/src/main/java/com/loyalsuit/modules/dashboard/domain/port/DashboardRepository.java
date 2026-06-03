package com.loyalsuit.modules.dashboard.domain.port;

import com.loyalsuit.modules.dashboard.application.dto.DashboardStats;

import java.util.UUID;

public interface DashboardRepository {
    DashboardStats statsForTenant(UUID tenantId);
}
