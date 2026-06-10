package com.loyalsuit.modules.notifications.api;

import com.loyalsuit.modules.notifications.application.NotificationService;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The inbox is each user's own — every authenticated role may read theirs (the service
 * scopes to the caller); only anonymous access is rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationAuthorizationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NotificationService notificationService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void anyAuthenticatedUserCanReadTheirInbox(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/notifications").header("Authorization", bearer(role)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/notifications/unread-count")).andExpect(status().is4xxClientError());
    }
}
