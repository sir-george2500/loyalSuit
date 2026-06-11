package com.loyalsuit.modules.privacy.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Account deletion is re-authenticated with the password — irreversible, so we confirm intent. */
@Data
public class DeleteAccountRequest {
    @NotBlank(message = "Your password is required")
    private String password;
}
