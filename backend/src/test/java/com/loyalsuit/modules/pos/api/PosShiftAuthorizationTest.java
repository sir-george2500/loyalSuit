package com.loyalsuit.modules.pos.api;

import com.loyalsuit.modules.pos.application.PosShiftService;
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
 * Cash-drawer shifts are operated by store-side roles (SUPER_ADMIN / TENANT_ADMIN /
 * STAFF). The service is mocked, so this asserts the authorization wall only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PosShiftAuthorizationTest {

    private static final Set<UserRole> STORE = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PosShiftService shiftService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void currentShift(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/pos/shifts/current").header("Authorization", bearer(role)))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void openShift(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/pos/shifts")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"openingFloat\":100.00}"))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void closeShift(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/pos/shifts/" + UUID.randomUUID() + "/close")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"countedCash\":350.00}"))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listShifts(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/pos/shifts").header("Authorization", bearer(role)))
                .andExpect(allowedFor(STORE, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/pos/shifts/current")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/pos/shifts")).andExpect(status().is4xxClientError());
    }
}
