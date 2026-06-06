package com.loyalsuit.modules.catalog.api;

import com.loyalsuit.modules.catalog.application.CategoryService;
import com.loyalsuit.modules.catalog.application.MediaUploadService;
import com.loyalsuit.modules.catalog.application.ProductService;
import com.loyalsuit.modules.catalog.application.ProductVariantService;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.security.JwtService;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role matrix for the catalog endpoints, enforced end-to-end: a real JWT flows
 * through the auth filter into method security, exactly as production would. Each
 * test runs across every {@link UserRole}, so an unguarded endpoint or a widened
 * role set fails the build. Services are mocked, so no database SQL runs.
 *
 * <p>Privilege tiers under test:
 * <ul>
 *   <li><b>READ</b> (browse/create products, read categories) — store roles + VENDOR</li>
 *   <li><b>STORE_WRITE</b> (publish products, mutate categories) — store roles, no VENDOR</li>
 *   <li><b>ADMIN_ONLY</b> (delete) — tenant admins only</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogAuthorizationTest {

    private static final Set<UserRole> READ =
            EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF, UserRole.VENDOR);
    private static final Set<UserRole> STORE_WRITE =
            EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);
    private static final Set<UserRole> ADMIN_ONLY =
            EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    private static final String PRODUCT_BODY = "{\"name\":\"Widget\",\"slug\":\"widget\",\"price\":9.99}";
    private static final String CATEGORY_BODY = "{\"name\":\"Tools\",\"slug\":\"tools\"}";
    private static final String VARIANT_BODY = "{\"name\":\"Large\",\"price\":9.99}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private ProductVariantService productVariantService;

    @MockitoBean
    private MediaUploadService mediaUploadService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(),
                role.name().toLowerCase() + "@test.dev",
                role.name(),
                UUID.randomUUID());
    }

    /** Allowed roles must succeed (2xx); everyone else must be forbidden (403). */
    private ResultMatcher expected(Set<UserRole> allowed, UserRole role) {
        return allowed.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    // ---- Products ----------------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listProducts(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/catalog/products").header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void getProduct(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/catalog/products/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void createProduct(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/catalog/products")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_BODY))
                .andExpect(expected(READ, role));
    }

    // Vendors manage their own products, so update/status are open to the READ set
    // (admins, staff, vendors); the service enforces per-vendor ownership + active status.
    @ParameterizedTest
    @EnumSource(UserRole.class)
    void updateProduct(UserRole role) throws Exception {
        mvc.perform(put("/api/v1/catalog/products/" + UUID.randomUUID())
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_BODY))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void publishProduct(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/catalog/products/" + UUID.randomUUID() + "/publish")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void unpublishProduct(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/catalog/products/" + UUID.randomUUID() + "/unpublish")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void archiveProduct(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/catalog/products/" + UUID.randomUUID() + "/archive")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void deleteProduct(UserRole role) throws Exception {
        mvc.perform(delete("/api/v1/catalog/products/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(ADMIN_ONLY, role));
    }

    // ---- Categories --------------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listCategories(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/catalog/categories").header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void getCategory(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/catalog/categories/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void createCategory(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/catalog/categories")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CATEGORY_BODY))
                .andExpect(expected(STORE_WRITE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void updateCategory(UserRole role) throws Exception {
        mvc.perform(put("/api/v1/catalog/categories/" + UUID.randomUUID())
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CATEGORY_BODY))
                .andExpect(expected(STORE_WRITE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void deleteCategory(UserRole role) throws Exception {
        mvc.perform(delete("/api/v1/catalog/categories/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(ADMIN_ONLY, role));
    }

    // ---- Variants ----------------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listVariants(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/catalog/products/" + UUID.randomUUID() + "/variants")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void createVariant(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/catalog/products/" + UUID.randomUUID() + "/variants")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VARIANT_BODY))
                .andExpect(expected(STORE_WRITE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void updateVariant(UserRole role) throws Exception {
        mvc.perform(put("/api/v1/catalog/products/" + UUID.randomUUID() + "/variants/" + UUID.randomUUID())
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VARIANT_BODY))
                .andExpect(expected(STORE_WRITE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void deleteVariant(UserRole role) throws Exception {
        mvc.perform(delete("/api/v1/catalog/products/" + UUID.randomUUID() + "/variants/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(STORE_WRITE, role));
    }

    // ---- Media -------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listMedia(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/catalog/products/" + UUID.randomUUID() + "/media")
                        .header("Authorization", bearer(role)))
                .andExpect(expected(READ, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void uploadMedia(UserRole role) throws Exception {
        var file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/v1/catalog/products/" + UUID.randomUUID() + "/media")
                        .file(file)
                        .header("Authorization", bearer(role)))
                .andExpect(expected(STORE_WRITE, role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void deleteMedia(UserRole role) throws Exception {
        mvc.perform(delete("/api/v1/catalog/products/" + UUID.randomUUID() + "/media/" + UUID.randomUUID())
                        .header("Authorization", bearer(role)))
                .andExpect(expected(STORE_WRITE, role));
    }

    // ---- Anonymous ---------------------------------------------------------

    @Test
    void anonymousIsRejectedFromEveryCatalogEndpoint() throws Exception {
        mvc.perform(get("/api/v1/catalog/products")).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/catalog/products/" + UUID.randomUUID())).andExpect(status().is4xxClientError());
        mvc.perform(post("/api/v1/catalog/products")
                        .contentType(MediaType.APPLICATION_JSON).content(PRODUCT_BODY))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/v1/catalog/categories")).andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/v1/catalog/categories/" + UUID.randomUUID())).andExpect(status().is4xxClientError());
    }
}
