package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.modules.fulfilment.application.DeliveryService;
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
 * The dispatch desk is operational, so it's open to store-side staff (SUPER_ADMIN /
 * TENANT_ADMIN / STAFF); vendors, customers, and delivery agents have no dispatch access.
 * The service is mocked — this asserts the authorization wall only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDeliveryAuthorizationTest {

    private static final Set<UserRole> STORE = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DeliveryService deliveryService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listDeliveries(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/deliveries").header("Authorization", bearer(role)))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void assignDelivery(UserRole role) throws Exception {
        String body = "{\"orderNumber\":\"ORD-1001\",\"agentId\":\"" + UUID.randomUUID() + "\"}";
        mvc.perform(post("/api/v1/admin/deliveries/assign")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void advanceDelivery(UserRole role) throws Exception {
        String body = "{\"status\":\"PICKED_UP\"}";
        mvc.perform(patch("/api/v1/admin/deliveries/" + UUID.randomUUID() + "/status")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(STORE, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/deliveries")).andExpect(status().is4xxClientError());
    }
}
