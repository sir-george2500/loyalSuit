package com.loyalsuit.modules.inventory.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String address;

    private boolean isDefault = false;
}
