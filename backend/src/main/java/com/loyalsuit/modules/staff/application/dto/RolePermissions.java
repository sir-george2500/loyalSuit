package com.loyalsuit.modules.staff.application.dto;

import com.loyalsuit.modules.users.domain.UserRole;

import java.util.List;

/** What a single role is allowed to do — one row of the Roles & Permissions matrix. */
public record RolePermissions(UserRole role, String label, String description, List<String> permissions) {}
