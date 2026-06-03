package com.loyalsuit.modules.users.domain.port;

import com.loyalsuit.modules.users.domain.AppUser;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository {
    AppUser save(AppUser user);
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findById(UUID id);
    boolean existsByEmail(String email);
}
