package com.loyalsuit.modules.audit.api;

import com.loyalsuit.modules.audit.application.AuditService;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The audit trail is sensitive: only tenant owners may read it. Verifies the role
 * matrix end-to-end with real JWTs. The service is mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditAuthorizationTest {

    private static final Set<UserRole> ALLOWED =
            EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void stub() {
        when(auditService.query(any(), any(), any())).thenReturn(Page.empty());
    }

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher expected(UserRole role) {
        return ALLOWED.contains(role) ? status().isOk() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listAuditLog(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/audit").header("Authorization", bearer(role)))
                .andExpect(expected(role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/audit")).andExpect(status().is4xxClientError());
    }
}
