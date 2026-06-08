package com.loyalsuit.modules.pos.api;

import com.loyalsuit.modules.pos.application.PosService;
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
 * The POS terminal is staffed by store-side roles (SUPER_ADMIN / TENANT_ADMIN / STAFF);
 * vendors, customers, and delivery agents have no register access. The service is mocked,
 * so no SQL runs — this asserts the authorization wall only.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PosAuthorizationTest {

    private static final Set<UserRole> STORE = EnumSet.of(
            UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private PosService posService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher allowedFor(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void searchProducts(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/pos/products").header("Authorization", bearer(role)))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void completeSale(UserRole role) throws Exception {
        String body = "{\"clientSaleId\":\"" + UUID.randomUUID()
                + "\",\"items\":[{\"productId\":\"" + UUID.randomUUID()
                + "\",\"quantity\":1}],\"amountTendered\":10.00}";
        mvc.perform(post("/api/v1/pos/sales")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(allowedFor(STORE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listSales(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/pos/sales").header("Authorization", bearer(role)))
                .andExpect(allowedFor(STORE, role));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/pos/products")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/pos/sales")).andExpect(status().is4xxClientError());
    }
}
