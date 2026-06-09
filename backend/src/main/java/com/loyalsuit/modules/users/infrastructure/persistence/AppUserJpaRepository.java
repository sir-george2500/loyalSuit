package com.loyalsuit.modules.users.infrastructure.persistence;

import com.loyalsuit.modules.users.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AppUserJpaRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findByIdInAndTenantId(Collection<UUID> ids, UUID tenantId);
    boolean existsByEmail(String email);
}
