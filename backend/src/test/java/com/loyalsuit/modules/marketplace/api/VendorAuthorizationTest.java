package com.loyalsuit.modules.marketplace.api;

import com.loyalsuit.modules.marketplace.application.VendorService;
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
 * Admin vendor management is owner-only (approval grants a role). Vendor self-service
 * (apply / me) is open to any authenticated user. Service is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VendorAuthorizationTest {

    private static final Set<UserRole> OWNERS = EnumSet.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private VendorService vendorService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private ResultMatcher ownerOnly(UserRole role) {
        return OWNERS.contains(role) ? status().is2xxSuccessful() : status().isForbidden();
    }

    // ---- admin (owner-only) -------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void listVendors(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/vendors").header("Authorization", bearer(role)))
                .andExpect(ownerOnly(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void approveVendor(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/admin/vendors/" + UUID.randomUUID() + "/approve")
                        .header("Authorization", bearer(role)))
                .andExpect(ownerOnly(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void suspendVendor(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/admin/vendors/" + UUID.randomUUID() + "/suspend")
                        .header("Authorization", bearer(role)))
                .andExpect(ownerOnly(role));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void setCommission(UserRole role) throws Exception {
        mvc.perform(patch("/api/v1/admin/vendors/" + UUID.randomUUID() + "/commission")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rate\":12.5}"))
                .andExpect(ownerOnly(role));
    }

    // ---- self-service (any authenticated user) ------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void apply_isOpenToAnyAuthenticatedUser(UserRole role) throws Exception {
        mvc.perform(post("/api/v1/vendor/apply")
                        .header("Authorization", bearer(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"storeName\":\"My Shop\"}"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void apply_anonymousIsRejected() throws Exception {
        mvc.perform(post("/api/v1/vendor/apply")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"storeName\":\"My Shop\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void adminVendors_anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/admin/vendors")).andExpect(status().is4xxClientError());
    }
}
