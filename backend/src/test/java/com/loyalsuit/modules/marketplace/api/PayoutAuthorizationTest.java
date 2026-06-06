package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.modules.marketplace.application.PayoutService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Payouts: a vendor requests and tracks their own payouts (VENDOR-only); paying or
 * rejecting them disburses funds, so it is owner-only (SUPER_ADMIN / TENANT_ADMIN).
 * Service is mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PayoutAuthorizationTest {

    private static final Set<UserRole> OWNERS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);
    private static final Set<UserRole> VENDOR_ONLY = EnumSet.of(UserRole.VENDOR);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PayoutService payoutService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    // ---- vendor self-service (VENDOR only) ----------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myBalance(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/vendor/payouts/balance").header("Authorization", bearer(role)))
                .andExpect(allowedFor(VENDOR_ONLY, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void requestPayout(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/vendor/payouts")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10.00}"))
                .andExpect(allowedFor(VENDOR_ONLY, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myPayouts(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/vendor/payouts").header("Authorization", bearer(role)))
                .andExpect(allowedFor(VENDOR_ONLY, role));
    }

    // ---- admin (owner-only) -------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listPayouts(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/payouts").header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void payPayout(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/admin/payouts/" + UUID.randomUUID() + "/pay")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"CASH-1\"}"))
                .andExpect(allowedFor(OWNERS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void rejectPayout(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/admin/payouts/" + UUID.randomUUID() + "/reject")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"No\"}"))
                .andExpect(allowedFor(OWNERS, role));
    }

    // ---- anonymous ----------------------------------------------------------

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/vendor/payouts/balance")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/admin/payouts")).andExpect(status().is4xxClientError());
    }
}
