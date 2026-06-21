package com.loyalsuit.modules.marketplace.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * A vendor's self-edit of their storefront profile. The slug is intentionally absent —
 * it is the public storefront URL and stays stable once a vendor is approved, so links
 * and SEO don't break. The logo is uploaded via a separate multipart endpoint.
 */
@Data
public class UpdateStorefrontRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 255)
    private String storeName;

    @Size(max = 2000)
    private String description;
}
