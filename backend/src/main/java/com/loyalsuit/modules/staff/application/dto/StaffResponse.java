package com.loyalsuit.modules.staff.application.dto;

import com.loyalsuit.modules.users.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

/** A tenant staff member (owner / admin / staff) for the Staff & Roles roster. */
public record StaffResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role,
        boolean active,
        Instant joinedAt) {}
