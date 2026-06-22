package com.loyalsuit.modules.staff.application;

import com.loyalsuit.common.exception.BusinessException;
import com.loyalsuit.common.exception.NotFoundException;
import com.loyalsuit.common.response.PageResponse;
import com.loyalsuit.modules.staff.application.dto.StaffResponse;
import com.loyalsuit.modules.users.domain.AppUser;
import com.loyalsuit.modules.users.domain.UserRole;
import com.loyalsuit.modules.users.domain.port.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages a tenant's back-office team: who is on staff, what role they hold, and whether
 * their account is active. The store owner (SUPER_ADMIN) is shown but protected — their
 * role can't be reassigned and they can't be deactivated. Acting users can't change their
 * own role or lock themselves out.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    /** Roles that constitute "staff" — shown on the roster. */
    private static final List<UserRole> STAFF_ROLES =
            List.of(UserRole.SUPER_ADMIN, UserRole.TENANT_ADMIN, UserRole.STAFF);

    /** Roles an admin may assign through this screen (never SUPER_ADMIN / VENDOR / CUSTOMER). */
    private static final Set<UserRole> ASSIGNABLE = EnumSet.of(UserRole.TENANT_ADMIN, UserRole.STAFF);

    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> list(UUID tenantId, Pageable pageable) {
        return new PageResponse<>(
                appUserRepository.findByTenantIdAndRoleIn(tenantId, STAFF_ROLES, pageable).map(this::toResponse));
    }

    @Transactional
    public StaffResponse changeRole(UUID tenantId, UUID actingUserId, UUID targetId, UserRole role) {
        if (!ASSIGNABLE.contains(role)) {
            throw new BusinessException("That role cannot be assigned here.");
        }
        AppUser user = require(tenantId, targetId);
        if (user.getId().equals(actingUserId)) {
            throw new BusinessException("You cannot change your own role.");
        }
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new BusinessException("The store owner's role cannot be changed.");
        }
        user.setRole(role);
        return toResponse(appUserRepository.save(user));
    }

    @Transactional
    public StaffResponse setActive(UUID tenantId, UUID actingUserId, UUID targetId, boolean active) {
        AppUser user = require(tenantId, targetId);
        if (user.getId().equals(actingUserId)) {
            throw new BusinessException("You cannot change your own account status.");
        }
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            throw new BusinessException("The store owner's account cannot be deactivated.");
        }
        user.setActive(active);
        return toResponse(appUserRepository.save(user));
    }

    private AppUser require(UUID tenantId, UUID id) {
        return appUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Staff member", id));
    }

    private StaffResponse toResponse(AppUser u) {
        return new StaffResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
