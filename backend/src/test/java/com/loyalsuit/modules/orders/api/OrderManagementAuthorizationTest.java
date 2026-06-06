package com.loyalsuit.modules.orders.api;

import com.loyalsuit.modules.orders.application.OrderManagementService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Order management is staff-facing: store roles only; customers and vendors are
 * forbidden. The service is mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderManagementAuthorizationTest {

    private static final Set<UserRole> STORE = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private OrderManagementService orderManagementService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher expected(UserRole role) {
        return STORE.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listOrders(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/orders").header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void getOrder(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/orders/" + UUID.randomUUID()).header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void transitionOrder(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/orders/" + UUID.randomUUID() + "/status")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(expected(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void markPaid(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/orders/" + UUID.randomUUID() + "/mark-paid")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/orders")).andExpect(status().is4xxClientError());
    }
}
