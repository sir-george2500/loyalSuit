package com.loyalsuit.modules.inventory.api;

import com.loyalsuit.modules.inventory.application.WarehouseService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role matrix for warehouse endpoints. Warehouses are operational config: store
 * roles read and edit; only tenant admins may delete. Vendors and customers are
 * excluded entirely. The service is mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WarehouseAuthorizationTest {

    private static final Set<UserRole> STORE = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);
    private static final Set<UserRole> ADMIN_ONLY = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    private static final String BODY = "{\"name\":\"Main\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private WarehouseService warehouseService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher expected(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listWarehouses(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/inventory/warehouses").header("Authorization", bearer(role)))
                .andExpect(expected(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void getWarehouse(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/inventory/warehouses/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void createWarehouse(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/inventory/warehouses")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(expected(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void updateWarehouse(UserRole role) throws Exception {
        mvc.perform(put("/api/v1/inventory/warehouses/" + UUID.randomUUID())
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(expected(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void deleteWarehouse(UserRole role) throws Exception {
        mvc.perform(delete("/api/v1/inventory/warehouses/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(ADMIN_ONLY, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/inventory/warehouses")).andExpect(status().is4xxClientError());
    }
}
