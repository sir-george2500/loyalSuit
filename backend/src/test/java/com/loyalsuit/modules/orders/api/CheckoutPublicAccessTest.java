package com.loyalsuit.modules.orders.api;

import com.loyalsuit.modules.orders.application.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guest cash checkout must be reachable anonymously (no JWT). A missing cart token
 * is a 400, and an invalid body is a 400 — never a 401/403. The service is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutPublicAccessTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CheckoutService checkoutService;

    private static final String VALID_BODY =
            "{\"customerName\":\"Jane\",\"addressLine1\":\"1 Market St\",\"city\":\"Springfield\"}";

    @Test
    void checkout_isPublic_withCartTokenAndValidBody() throws Exception {
        mvc.perform(post("/api/v1/store/acme/checkout")
                        .header("X-Cart-Token", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    void checkout_withoutCartToken_is400_notUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/store/acme/checkout")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_withInvalidBody_is400() throws Exception {
        mvc.perform(post("/api/v1/store/acme/checkout")
                        .header("X-Cart-Token", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
