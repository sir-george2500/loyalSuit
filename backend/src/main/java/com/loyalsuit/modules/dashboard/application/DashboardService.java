package com.loyalsuit.modules.dashboard.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.dashboard.application.dto.DashboardStats;
import com.loyalsuit.modules.dashboard.domain.port.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    public DashboardStats getStats(UUID tenantId) {
        if (tenantId == null) {
            throw new BusinessException("No tenant associated with this account", HttpStatus.FORBIDDEN);
        }
        return dashboardRepository.statsForTenant(tenantId);
    }
}
