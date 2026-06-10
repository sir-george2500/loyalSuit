package com.loyalsuit.modules.loyalty.api;

import com.loyalsuit.modules.loyalty.application.LoyaltyService;
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

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loyalty is a customer-facing feature: only a CUSTOMER may read their own points. The
 * service is mocked — this asserts the authorization wall.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoyaltyAuthorizationTest {

    private static final Set<UserRole> CUSTOMER = EnumSet.of(UserRole.CUSTOMER);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private LoyaltyService loyaltyService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myBalance(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/loyalty/me").header("Authorization", bearer(role)))
                .andExpect(allowedFor(CUSTOMER, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myHistory(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/loyalty/me/history").header("Authorization", bearer(role)))
                .andExpect(allowedFor(CUSTOMER, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/loyalty/me")).andExpect(status().is4xxClientError());
    }
}
