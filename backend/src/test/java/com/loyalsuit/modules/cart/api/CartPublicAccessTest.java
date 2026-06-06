package com.loyalsuit.modules.cart.api;

import com.loyalsuit.modules.cart.application.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guest carts must work without authentication: the cart mutation endpoints have to
 * be reachable anonymously (the SecurityConfig permit), and a missing cart token is
 * a 400 — not a 401/403. The service is mocked, so no Redis or SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartPublicAccessTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CartService cartService;

    @Test
    void viewCart_isPublic_withToken() throws Exception {
        mvc.perform(get("/api/v1/store/acme/cart").header("X-Cart-Token", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void addItem_isPublic_withToken() throws Exception {
        String body = "{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}";
        mvc.perform(post("/api/v1/store/acme/cart/items")
                        .header("X-Cart-Token", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void cart_isReachableAnonymously_butRequiresAToken() throws Exception {
        // No JWT and no token: reaches the controller (not 401/403) and fails as 400.
        mvc.perform(get("/api/v1/store/acme/cart")).andExpect(status().isBadRequest());
    }
}
