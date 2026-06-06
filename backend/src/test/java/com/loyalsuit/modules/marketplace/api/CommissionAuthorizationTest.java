package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.modules.marketplace.application.CommissionService;
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
 * Commission ledger access: a vendor reads only their own earnings (VENDOR-only); the
 * tenant-wide ledger is owner-only (financial data, same tier as vendor management).
 * Service is mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommissionAuthorizationTest {

    private static final Set<UserRole> OWNERS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);
    private static final Set<UserRole> VENDOR_ONLY = EnumSet.of(UserRole.VENDOR);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CommissionService commissionService;

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
    void myEarnings(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/vendor/earnings").header("Authorization", bearer(role)))
                .andExpect(allowedFor(VENDOR_ONLY, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myCommissionLedger(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/vendor/commissions").header("Authorization", bearer(role)))
                .andExpect(allowedFor(VENDOR_ONLY, role));
    }

    // ---- admin ledger (owner-only) ------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void tenantLedger(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/commissions").header("Authorization", bearer(role)))
                .andExpect(allowedFor(OWNERS, role));
    }

    // ---- anonymous ----------------------------------------------------------

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/vendor/earnings")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/vendor/commissions")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/admin/commissions")).andExpect(status().is4xxClientError());
    }
}
