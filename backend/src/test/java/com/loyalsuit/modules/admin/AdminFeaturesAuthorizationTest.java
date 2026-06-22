package com.loyalsuit.modules.admin;

import com.loyalsuit.modules.apikeys.application.ApiKeyService;
import com.loyalsuit.modules.billing.application.BillingService;
import com.loyalsuit.modules.customers.application.CustomerService;
import com.loyalsuit.modules.settings.application.NotificationSettingsService;
import com.loyalsuit.modules.staff.application.RolePermissionCatalog;
import com.loyalsuit.modules.staff.application.StaffService;
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
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the security wiring of the admin back-office features. The "Customers" and
 * "Notifications" surfaces are open to any store staff; the finance, staff-management
 * and API-key surfaces are owner-only. Services are mocked — only authorization is asserted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminFeaturesAuthorizationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private CustomerService customerService;
    @MockitoBean private StaffService staffService;
    @MockitoBean private RolePermissionCatalog rolePermissionCatalog;
    @MockitoBean private BillingService billingService;
    @MockitoBean private NotificationSettingsService notificationSettingsService;
    @MockitoBean private ApiKeyService apiKeyService;

    private String bearer(UserRole role) {
        return "Bearer " + jwtService.issueToken(
                UUID.randomUUID(), role.name().toLowerCase() + "@test.dev", role.name(), UUID.randomUUID());
    }

    private boolean isStoreStaff(UserRole role) {
        return role == UserRole.SUPER_ADMIN || role == UserRole.TENANT_ADMIN || role == UserRole.STAFF;
    }

    private boolean isOwner(UserRole role) {
        return role == UserRole.SUPER_ADMIN || role == UserRole.TENANT_ADMIN;
    }

    private ResultMatcher allowIf(boolean allowed) {
        return allowed ? status().is2xxSuccessful() : status().isForbidden();
    }

    // ---- anonymous is always denied -----------------------------------------

    @Test
    void adminSurfaces_denyAnonymous() throws Exception {
        // Stateless JWT security denies unauthenticated requests with 403 (no 401 entry point).
        mvc.perform(get("/api/v1/admin/customers")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/staff")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/roles")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/payments")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/invoices")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/settings/notifications")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/api-keys")).andExpect(status().isForbidden());
    }

    // ---- store-staff surfaces -----------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void customers_visibleToStoreStaff(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/customers").header("Authorization", bearer(role)))
                .andExpect(allowIf(isStoreStaff(role)));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void notificationSettings_visibleToStoreStaff(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/settings/notifications").header("Authorization", bearer(role)))
                .andExpect(allowIf(isStoreStaff(role)));
    }

    // ---- owner-only surfaces ------------------------------------------------

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void staff_isOwnerOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/staff").header("Authorization", bearer(role)))
                .andExpect(allowIf(isOwner(role)));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void roles_isOwnerOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/roles").header("Authorization", bearer(role)))
                .andExpect(allowIf(isOwner(role)));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void payments_isOwnerOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/payments").header("Authorization", bearer(role)))
                .andExpect(allowIf(isOwner(role)));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void invoices_isOwnerOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/invoices").header("Authorization", bearer(role)))
                .andExpect(allowIf(isOwner(role)));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void apiKeys_isOwnerOnly(UserRole role) throws Exception {
        mvc.perform(get("/api/v1/admin/api-keys").header("Authorization", bearer(role)))
                .andExpect(allowIf(isOwner(role)));
    }
}
