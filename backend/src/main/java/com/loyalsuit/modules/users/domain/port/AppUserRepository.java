package com.loyalsuit.modules.users.domain.port;

import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository {
    AppUser save(AppUser user);
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findById(UUID id);
    /** Batch lookup by id within a tenant — one query for many ids (e.g. roster names). */
    List<AppUser> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);
    boolean existsByEmail(String email);

    /** Customers of a tenant, optionally filtered by a name/email search term (null = all). */
    Page<AppUser> findCustomers(UUID tenantId, String search, Pageable pageable);

    /** Tenant users holding any of the given roles — the admin "Staff & Roles" roster. */
    Page<AppUser> findByTenantIdAndRoleIn(UUID tenantId, Collection<UserRole> roles, Pageable pageable);

    /** A single tenant-scoped user (admin actions must never cross tenants). */
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);
}
