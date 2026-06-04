package com.loyalsuit.modules.tenants.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for the tenant setup wizard: company profile, localization, and the
 * first warehouse. Currency and country are constrained to their ISO code shapes.
 */
@Data
public class CompleteOnboardingRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 255)
    private String businessName;

    @Size(max = 40)
    private String phone;

    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country must be a 2-letter ISO code")
    private String country;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;

    @NotBlank(message = "Timezone is required")
    @Size(max = 64)
    private String timezone;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 255)
    private String warehouseName;

    @Size(max = 1000)
    private String warehouseAddress;
}
