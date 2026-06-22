package com.loyalsuit.modules.platform.api;

import com.loyalsuit.modules.featureflags.application.FeatureFlagService;
import com.loyalsuit.modules.plans.application.PlanService;
import com.loyalsuit.modules.platform.application.PlatformTenantService;
import com.loyalsuit.modules.platform.application.SystemHealthService;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Platform administration (tenants, plans, system health, feature flags) is reserved for
 * the platform operator: SUPER_ADMIN only. Every other role — including a tenant's own
 * admin — must be forbidden. Services are mocked; only authorization is asserted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformAuthorizationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private PlatformTenantService platformTenantService;
    @MockitoBean private SystemHealthService systemHealthService;
    @MockitoBean private PlanService planService;
    @MockitoBean private FeatureFlagService featureFlagService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher superAdminOnly(UserRole role) {
        return role == UserRole.SUPER_ADMIN ? status().is2xxSuccessful() : status().isForbidden();
    }

    @Test
    void platformSurfaces_denyAnonymous() throws Exception {
        mvc.perform(get("/api/v1/platform/tenants")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/platform/plans")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/platform/system")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/platform/flags")).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void tenants_areSuperAdminOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/platform/tenants").header("Authorization", bearer(role)))
                .andExpect(superAdminOnly(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void plans_areSuperAdminOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/platform/plans").header("Authorization", bearer(role)))
                .andExpect(superAdminOnly(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void systemHealth_isSuperAdminOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/platform/system").header("Authorization", bearer(role)))
                .andExpect(superAdminOnly(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void featureFlags_areSuperAdminOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/platform/flags").header("Authorization", bearer(role)))
                .andExpect(superAdminOnly(role));
    }
}
