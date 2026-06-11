package com.loyalsuit.modules.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Disabling 2FA is re-authenticated with the account password. */
@Data
public class TwoFactorDisableRequest {
    @NotBlank(message = "Your password is required")
    private String password;
}
