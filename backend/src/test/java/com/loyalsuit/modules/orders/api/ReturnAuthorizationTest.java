package com.loyalsuit.modules.orders.api;

import com.loyalsuit.modules.orders.application.ReturnService;
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
 * Admin return management is store-role only; the guest return-request endpoint is
 * public. The service is mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReturnAuthorizationTest {

    private static final Set<UserRole> STORE = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReturnService returnService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher expected(UserRole role) {
        return STORE.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listReturns(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/returns").header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void approveReturn(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/returns/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void rejectReturn(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/returns/" + UUID.randomUUID() + "/reject")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"no\"}"))
                .andExpect(expected(role));
    }

    @Test
    void guestReturnRequest_isPublic() throws Exception {
        String body = "{\"email\":\"jane@acme.dev\",\"reason\":\"Wrong size\"}";
        mvc.perform(post("/api/v1/store/acme/orders/ORD-1/returns")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void adminReturns_anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/returns")).andExpect(status().is4xxClientError());
    }
}
