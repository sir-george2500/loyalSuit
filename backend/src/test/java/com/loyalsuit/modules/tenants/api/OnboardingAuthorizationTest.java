package com.loyalsuit.modules.tenants.api;

import com.loyalsuit.modules.tenants.application.OnboardingService;
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
 * Role matrix for the onboarding endpoints. Only tenant owners run setup, so just
 * SUPER_ADMIN and TENANT_ADMIN may reach these; every other role is forbidden. The
 * service is mocked, so no database SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingAuthorizationTest {

    private static final Set<UserRole> ALLOWED =
            EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    private static final String VALID_BODY =
            "{\"businessName\":\"Acme\",\"currency\":\"USD\",\"timezone\":\"UTC\",\"warehouseName\":\"Main\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private OnboardingService onboardingService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher expected(UserRole role) {
        return ALLOWED.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void getStatus(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/onboarding/status").header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void complete(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/onboarding/complete")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(expected(role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/onboarding/status")).andExpect(status().is4xxClientError());
        mvc.perform(post("/api/v1/onboarding/complete")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().is4xxClientError());
    }
}
