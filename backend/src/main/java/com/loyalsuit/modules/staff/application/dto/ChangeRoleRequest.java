package com.loyalsuit.modules.staff.application.dto;

import com.loyalsuit.modules.users.domain.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @NotNull
    private UserRole role;
}
