package com.loyalsuit.modules.apikeys.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApiKeyRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
}
