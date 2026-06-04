package com.loyalsuit.modules.dashboard.api;

import com.loyalsuit.modules.dashboard.application.DashboardService;
import com.loyalsuit.modules.dashboard.application.dto.DashboardStats;
import com.loyalsuit.modules.dashboard.application.dto.KpiMetric;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the role matrix for the dashboard endpoint end-to-end: a real JWT flows
 * through the auth filter into method security, exercising authorization exactly as
 * production would. The service is mocked so no database SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DashboardService dashboardService;

    private String tokenFor(UserRole role) {
        return jwtService.issueToken(
                UUID.randomUUID(),
                role.name().toLowerCase() + "@test.dev",
                role.name(),
                UUID.randomUUID());
    }

    private void stubStats() {
        when(dashboardService.getStats(any())).thenReturn(new DashboardStats(
                KpiMetric.of(BigDecimal.ZERO, BigDecimal.ZERO),
                KpiMetric.of(0, 0),
                KpiMetric.of(0, 0),
                BigDecimal.ZERO, 0, 0, 0, 0,
                List.of(), List.of(), List.of()));
    }

    @Test
    void superAdmin_canAccess() throws Exception {
        stubStats();
        mvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenFor(UserRole.SUPER_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdmin_canAccess() throws Exception {
        stubStats();
        mvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenFor(UserRole.TENANT_ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void staff_canAccess() throws Exception {
        stubStats();
        mvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenFor(UserRole.STAFF)))
                .andExpect(status().isOk());
    }

    @Test
    void customer_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenFor(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void vendor_isForbidden() throws Exception {
        mvc.perform(get("/api/v1/dashboard/stats")
                        .header("Authorization", "Bearer " + tokenFor(UserRole.VENDOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void noToken_isUnauthorizedOrForbidden() throws Exception {
        mvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().is4xxClientError());
    }
}
