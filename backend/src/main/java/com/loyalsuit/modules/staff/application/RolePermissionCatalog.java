package com.loyalsuit.modules.staff.application;

import com.loyalsuit.modules.staff.application.dto.RolePermissions;
import com.loyalsuit.modules.users.domain.UserRole;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authoritative description of what each role may do. The runtime enforcement lives in the
 * {@code @PreAuthorize} guards on the controllers; this catalogue is the human-readable mirror
 * of that model, surfaced on the admin "Roles & Permissions" screen. Roles are fixed in code,
 * so the matrix is static (no per-tenant overrides yet).
 */
@Service
public class RolePermissionCatalog {

    public List<RolePermissions> all() {
        return List.of(
                new RolePermissions(UserRole.SUPER_ADMIN, "Store Owner",
                        "Full control of the store and platform administration.",
                        List.of(
                                "Manage all products, categories and inventory",
                                "Manage orders, returns and deliveries",
                                "Manage vendors, commissions and payouts",
                                "Manage staff, roles and permissions",
                                "Manage billing, invoices and subscription plans",
                                "Manage platform tenants, feature flags and API keys",
                                "View all reports and the audit log")),
                new RolePermissions(UserRole.TENANT_ADMIN, "Administrator",
                        "Runs the store day to day, short of platform-level controls.",
                        List.of(
                                "Manage all products, categories and inventory",
                                "Manage orders, returns and deliveries",
                                "Manage vendors, commissions and payouts",
                                "Manage staff and roles",
                                "Manage billing and invoices",
                                "View reports and the audit log")),
                new RolePermissions(UserRole.STAFF, "Staff",
                        "Handles operational work; no team, billing or platform access.",
                        List.of(
                                "Manage products, categories and inventory",
                                "Process orders, returns and deliveries",
                                "View customers",
                                "Operate the POS terminal")),
                new RolePermissions(UserRole.VENDOR, "Vendor",
                        "Sells through the marketplace and manages only their own storefront.",
                        List.of(
                                "Manage own products and stock",
                                "View own orders, earnings and payouts",
                                "View own reviews",
                                "Edit own storefront profile")),
                new RolePermissions(UserRole.DELIVERY_AGENT, "Delivery Agent",
                        "Fulfils assigned deliveries.",
                        List.of(
                                "View assigned deliveries",
                                "Update delivery status")),
                new RolePermissions(UserRole.CUSTOMER, "Customer",
                        "Shops the storefront.",
                        List.of(
                                "Place and track own orders",
                                "Request returns",
                                "Review delivered products")));
    }
}
