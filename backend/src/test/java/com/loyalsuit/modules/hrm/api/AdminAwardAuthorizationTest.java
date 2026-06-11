package com.loyalsuit.modules.hrm.api;

import com.loyalsuit.modules.hrm.application.AwardService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HRM awards are owner-only (SUPER_ADMIN / TENANT_ADMIN); the plan gate is enforced separately
 * in the service. With the service mocked, this asserts the authorization wall only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAwardAuthorizationTest {

    private static final Set<UserRole> OWNERS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AwardService awardService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listAwards(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/awards").param("employeeId", UUID.randomUUID().toString())
                        .header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void recordAward(UserRole role) throws Exception {
        String body = "{\"employeeId\":\"" + UUID.randomUUID() + "\",\"title\":\"Employee of the Month\","
                + "\"category\":\"PERFORMANCE\",\"awardedOn\":\"2026-06-01\"}";
        mvc.perform(post("/api/v1/admin/awards")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void removeAward(UserRole role) throws Exception {
        mvc.perform(delete("/api/v1/admin/awards/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/awards").param("employeeId", UUID.randomUUID().toString()))
                .andExpect(status().is4xxClientError());
    }
}
