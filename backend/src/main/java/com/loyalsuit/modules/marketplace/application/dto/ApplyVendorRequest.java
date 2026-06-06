package com.loyalsuit.modules.marketplace.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplyVendorRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 255)
    private String storeName;

    @Size(max = 2000)
    private String description;
}
