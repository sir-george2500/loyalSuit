package com.loyalsuit.modules.storefront.api;

import com.loyalsuit.modules.storefront.application.MarketplaceService;
import com.loyalsuit.modules.storefront.application.StorefrontService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The storefront read endpoints must be reachable by anonymous visitors (no JWT),
 * while writes under /store must not exist as a public surface. The service is
 * mocked, so no SQL runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StorefrontPublicAccessTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StorefrontService storefrontService;

    @MockitoBean
    private MarketplaceService marketplaceService;

    @Test
    void marketplaceIndex_isPublic() throws Exception {
        mvc.perform(get("/api/v1/store")).andExpect(status().isOk());
    }

    @Test
    void marketplaceStore_isPublic() throws Exception {
        mvc.perform(get("/api/v1/marketplace")).andExpect(status().isOk());
    }

    @Test
    void marketplaceProducts_isPublic() throws Exception {
        mvc.perform(get("/api/v1/marketplace/products")).andExpect(status().isOk());
    }

    @Test
    void marketplaceSearch_isPublic() throws Exception {
        mvc.perform(get("/api/v1/marketplace/search").param("q", "mug")).andExpect(status().isOk());
    }

    @Test
    void marketplaceVendorStorefront_isPublic() throws Exception {
        mvc.perform(get("/api/v1/marketplace/vendors/bright-goods")).andExpect(status().isOk());
    }

    @Test
    void marketplaceVendorProducts_isPublic() throws Exception {
        mvc.perform(get("/api/v1/marketplace/vendors/bright-goods/products")).andExpect(status().isOk());
    }

    @Test
    void store_isPublic() throws Exception {
        mvc.perform(get("/api/v1/store/acme")).andExpect(status().isOk());
    }

    @Test
    void categories_isPublic() throws Exception {
        mvc.perform(get("/api/v1/store/acme/categories")).andExpect(status().isOk());
    }

    @Test
    void products_isPublic() throws Exception {
        mvc.perform(get("/api/v1/store/acme/products")).andExpect(status().isOk());
    }

    @Test
    void productDetail_isPublic() throws Exception {
        mvc.perform(get("/api/v1/store/acme/products/widget")).andExpect(status().isOk());
    }

    @Test
    void writesUnderStore_areNotPublic() throws Exception {
        // No public write surface exists; a POST must not be permitted (401/403/404/405).
        mvc.perform(post("/api/v1/store/acme/products/" + UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
}
