package com.loyalsuit.modules.fulfilment.api;

import com.loyalsuit.modules.fulfilment.application.DeliveryAgentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The delivery-agent roster is managed by tenant owners (SUPER_ADMIN / TENANT_ADMIN);
 * onboarding grants a role, so general staff and others are walled out. An agent reads
 * only their own profile. The service is mocked — this asserts the authorization wall.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryAgentAuthorizationTest {

    private static final Set<UserRole> ADMINS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);
    private static final Set<UserRole> AGENT = EnumSet.of(UserRole.DELIVERY_AGENT);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DeliveryAgentService agentService;

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
    void listRoster(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/delivery-agents").header("Authorization", bearer(role)))
                .andExpect(allowedFor(ADMINS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void registerAgent(UserRole role) throws Exception {
        String body = "{\"email\":\"rider@test.dev\",\"phone\":\"555-0100\",\"vehicleType\":\"MOTORBIKE\"}";
        mvc.perform(post("/api/v1/admin/delivery-agents")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(ADMINS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void updateAgent(UserRole role) throws Exception {
        String body = "{\"phone\":\"555-0199\",\"vehicleType\":\"CAR\"}";
        mvc.perform(put("/api/v1/admin/delivery-agents/" + UUID.randomUUID())
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(ADMINS, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myProfile(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/delivery/me").header("Authorization", bearer(role)))
                .andExpect(allowedFor(AGENT, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void myAssignments(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/delivery/assignments").header("Authorization", bearer(role)))
                .andExpect(allowedFor(AGENT, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void advanceMyAssignment(UserRole role) throws Exception {
        String body = "{\"status\":\"PICKED_UP\"}";
        mvc.perform(patch("/api/v1/delivery/assignments/" + UUID.randomUUID() + "/status")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(AGENT, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void completeMyAssignment(UserRole role) throws Exception {
        String body = "{\"recipientName\":\"Ada Lovelace\"}";
        mvc.perform(patch("/api/v1/delivery/assignments/" + UUID.randomUUID() + "/complete")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(AGENT, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/delivery-agents")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/delivery/me")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/delivery/assignments")).andExpect(status().is4xxClientError());
    }
}
