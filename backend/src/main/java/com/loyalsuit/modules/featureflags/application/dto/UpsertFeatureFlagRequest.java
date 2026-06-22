package com.loyalsuit.modules.featureflags.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpsertFeatureFlagRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[a-z0-9._-]+",
            message = "Key may contain only lowercase letters, digits, dots, dashes and underscores")
    private String flagKey;

    @Size(max = 500)
    private String description;

    private boolean enabled = false;
}
