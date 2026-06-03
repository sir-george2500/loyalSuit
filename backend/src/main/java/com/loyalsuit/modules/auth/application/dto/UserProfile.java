package com.loyalsuit.modules.auth.application.dto;

import com.loyalsuit.modules.users.domain.AppUser;
import lombok.Getter;

import java.util.UUID;

@Getter
public class UserProfile {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String avatarUrl;
    private final String role;
    private final UUID tenantId;

    public UserProfile(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.avatarUrl = user.getAvatarUrl();
        this.role = user.getRole().name();
        this.tenantId = user.getTenantId();
    }
}
