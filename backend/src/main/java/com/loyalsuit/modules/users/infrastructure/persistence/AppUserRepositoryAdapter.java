package com.loyalsuit.modules.users.infrastructure.persistence;

import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AppUserRepositoryAdapter implements AppUserRepository {

    private final AppUserJpaRepository jpa;

    @Override
    public AppUser save(AppUser user) {
        return jpa.save(user);
    }

    @Override
    public Optional<AppUser> findByEmail(String email) {
        return jpa.findByEmail(email);
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<AppUser> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId) {
        return ids.isEmpty() ? List.of() : jpa.findByIdInAndTenantId(ids, tenantId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }
}
