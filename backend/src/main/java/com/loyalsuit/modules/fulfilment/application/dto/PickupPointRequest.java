package com.loyalsuit.modules.fulfilment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Create or edit a pickup point. */
@Data
public class PickupPointRequest {

    @NotBlank(message = "A name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 500)
    private String address;

    @Size(max = 30)
    private String phone;
}
