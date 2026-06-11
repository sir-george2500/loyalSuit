package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.modules.hrm.application.PayrollService;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HRM payroll is owner-only (SUPER_ADMIN / TENANT_ADMIN); the plan gate is enforced separately
 * in the service. With the service mocked, this asserts the authorization wall only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPayrollAuthorizationTest {

    private static final Set<UserRole> OWNERS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PayrollService payrollService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listRuns(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/payroll/runs").header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void generateRun(UserRole role) throws Exception {
        String body = "{\"periodStart\":\"2026-06-01\",\"periodEnd\":\"2026-06-30\"}";
        mvc.perform(post("/api/v1/admin/payroll/runs")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void finaliseRun(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/admin/payroll/runs/" + UUID.randomUUID() + "/finalise")
                        .header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void payRun(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/admin/payroll/runs/" + UUID.randomUUID() + "/pay")
                        .header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/payroll/runs")).andExpect(status().is4xxClientError());
    }
}
