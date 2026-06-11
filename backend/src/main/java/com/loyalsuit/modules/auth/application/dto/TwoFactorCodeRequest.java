package com.loyalsuit.modules.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** A six-digit authenticator code, submitted to confirm 2FA enrolment. */
@Data
public class TwoFactorCodeRequest {
    @NotBlank(message = "A code is required")
    private String code;
}
