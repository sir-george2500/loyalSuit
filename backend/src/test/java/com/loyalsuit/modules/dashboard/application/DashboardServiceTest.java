package com.loyalsuit.modules.dashboard.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.modules.dashboard.application.dto.DashboardStats;
import com.loyalsuit.modules.dashboard.application.dto.KpiMetric;
import com.loyalsuit.modules.dashboard.domain.port.DashboardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getStats_returnsRepositoryResult_forTenant() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        var expected = new DashboardStats(
                KpiMetric.of(new BigDecimal("1000"), new BigDecimal("800")),
                KpiMetric.of(10, 5),
                KpiMetric.of(30, 20),
                new BigDecimal("100.00"),
                12, 8, 2, 3,
                List.of(), List.of(), List.of());
        when(dashboardRepository.statsForTenant(tenantId)).thenReturn(expected);

        // Act
        DashboardStats result = dashboardService.getStats(tenantId);

        // Assert
        assertThat(result.revenue().current()).isEqualByComparingTo("1000");
        assertThat(result.revenue().changePercent()).isEqualTo(25.0);
        assertThat(result.orders().current()).isEqualByComparingTo("10");
        assertThat(result.averageOrderValue()).isEqualByComparingTo("100.00");
        assertThat(result.lowStockCount()).isEqualTo(2);
    }

    @Test
    void getStats_throwsForbidden_whenTenantIdIsNull() {
        assertThatThrownBy(() -> dashboardService.getStats(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No tenant");
        verify(dashboardRepository, never()).statsForTenant(any());
    }

    @Test
    void kpiMetric_computesChangeAndHandlesZeroBaseline() {
        assertThat(KpiMetric.of(150, 100).changePercent()).isEqualTo(50.0);
        assertThat(KpiMetric.of(50, 100).changePercent()).isEqualTo(-50.0);
        // Growth from a zero baseline is reported as +100%, not a divide-by-zero.
        assertThat(KpiMetric.of(10, 0).changePercent()).isEqualTo(100.0);
        assertThat(KpiMetric.of(0, 0).changePercent()).isEqualTo(0.0);
    }
}
