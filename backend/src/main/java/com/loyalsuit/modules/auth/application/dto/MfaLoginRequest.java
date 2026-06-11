package com.loyalsuit.modules.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Completes a login challenge: the short-lived MFA token plus an authenticator or recovery code. */
@Data
public class MfaLoginRequest {
    @NotBlank(message = "The challenge token is required")
    private String mfaToken;

    @NotBlank(message = "A code is required")
    private String code;
}
