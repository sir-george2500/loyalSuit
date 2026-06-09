package com.loyalsuit.modules.users.domain.port;

import com.loyalsuit.modules.users.domain.AppUser;

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
}
